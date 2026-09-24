package com.github.yildizmy.service.push;

import com.github.yildizmy.domain.entity.PushSubscription;
import com.github.yildizmy.exception.WalletRuleException;
import com.github.yildizmy.repository.PushSubscriptionRepository;
import com.github.yildizmy.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.util.Base64;
import java.util.List;
import java.util.Locale;

/**
 * Browser subscriptions and delivery of the negative-balance alert by Web Push. Off unless the VAPID keys
 * are configured. The server only ever posts to the browsers' push services: the endpoint comes from the
 * client, so without the host list it would be a way to make the API call any URL (SSRF).
 */
@Slf4j
@Service
public class PushService {

    static final int MAX_PER_USER = 5;
    /** Push services of Chrome/Edge/Android, Firefox, Safari and Windows. */
    static final List<String> ALLOWED_HOSTS = List.of(
            "fcm.googleapis.com", "push.services.mozilla.com", "push.apple.com", "notify.windows.com");

    private final PushSubscriptionRepository repository;
    private final UserService userService;
    private final Clock clock;
    private final String publicKey;
    private final String privateKey;
    private final String subject;
    private final HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();

    public PushService(PushSubscriptionRepository repository, UserService userService, Clock clock,
                       @Value("${app.push.public-key:}") String publicKey,
                       @Value("${app.push.private-key:}") String privateKey,
                       @Value("${app.push.subject:mailto:no-reply@wallia.local}") String subject) {
        this.repository = repository;
        this.userService = userService;
        this.clock = clock;
        this.publicKey = publicKey;
        this.privateKey = privateKey;
        this.subject = subject;
    }

    public boolean enabled() {
        return !publicKey.isBlank() && !privateKey.isBlank();
    }

    /** What the browser needs to subscribe; null when push is off, so the screen hides the option. */
    public String publicKey() {
        return enabled() ? publicKey : null;
    }

    @Transactional(readOnly = true)
    public boolean hasSubscription(long userId) {
        return enabled() && repository.existsByUserId(userId);
    }

    /**
     * Saves the browser for this user and returns whether it now alerts this user.
     *
     * @param refresh the page re-sending what the browser already has: it updates keys but never moves a browser
     *                that belongs to another account, so whoever signs in next on a shared device does not take
     *                the previous user's alerts without asking
     */
    @Transactional
    public boolean subscribe(long userId, String endpoint, String p256dh, String auth, boolean refresh) {
        if (!enabled()) throw new WalletRuleException("PUSH_UNAVAILABLE");
        if (!allowedEndpoint(endpoint) || !validKey(p256dh, 65) || !validKey(auth, 16)) {
            throw new WalletRuleException("PUSH_INVALID_SUBSCRIPTION");
        }
        userService.lockForUpdate(userId);
        PushSubscription subscription = repository.findByEndpoint(endpoint).orElse(null);
        boolean mine = subscription != null && subscription.getUserId() == userId;
        if (!mine) {
            if (refresh) return false;
            // New or moved from another account: both count against this user's limit.
            if (repository.countByUserId(userId) >= MAX_PER_USER) throw new WalletRuleException("PUSH_LIMIT");
            if (subscription == null) {
                subscription = new PushSubscription();
                subscription.setEndpoint(endpoint);
            }
        }
        // An explicit "turn on here" takes the browser even if another account had it.
        subscription.setUserId(userId);
        subscription.setP256dh(p256dh);
        subscription.setAuth(auth);
        subscription.setCreatedAt(clock.instant());
        repository.save(subscription);
        return true;
    }

    @Transactional
    public void unsubscribe(long userId, String endpoint) {
        repository.findByEndpoint(endpoint)
                .filter(subscription -> subscription.getUserId() == userId)
                .ifPresent(repository::delete);
    }

    /**
     * Sends to every browser of the user; returns how many accepted it. A browser that uninstalled or
     * revoked (404/410) is removed. The text is fixed by the caller and never carries amounts, because
     * it shows on the lock screen.
     */
    public int send(long userId, String title, String body, String url) {
        if (!enabled()) return 0;
        String payload = "{\"title\":%s,\"body\":%s,\"url\":%s,\"tag\":\"wallia-alert\"}"
                .formatted(json(title), json(body), json(url));
        int delivered = 0;
        for (PushSubscription subscription : repository.findByUserId(userId)) {
            int status;
            try {
                status = post(subscription, WebPush.encrypt(subscription.getP256dh(), subscription.getAuth(),
                        payload.getBytes(StandardCharsets.UTF_8)));
            } catch (Exception exception) {
                if (exception instanceof InterruptedException) Thread.currentThread().interrupt();
                log.warn("Push failed for one browser: {}", exception.getClass().getSimpleName());
                continue;
            }
            if (status == 404 || status == 410) forget(subscription.getId());
            else if (status >= 200 && status < 300) delivered++;
            else log.warn("Push service answered {}", status);
        }
        return delivered;
    }

    private void forget(Long id) {
        try {
            repository.deleteById(id);
        } catch (org.springframework.dao.EmptyResultDataAccessException alreadyGone) {
            // Unsubscribed while we were sending: nothing left to do.
        }
    }

    /** One HTTP call to the push service; overridden in tests. */
    int post(PushSubscription subscription, byte[] body) throws Exception {
        HttpRequest request = HttpRequest.newBuilder(URI.create(subscription.getEndpoint()))
                .timeout(Duration.ofSeconds(8))
                .header("Authorization", WebPush.vapidAuthorization(subscription.getEndpoint(), publicKey, privateKey,
                        subject, clock.instant()))
                .header("Content-Encoding", "aes128gcm")
                .header("Content-Type", "application/octet-stream")
                // Kept one day by the push service if the device is off; after that the alert is stale anyway.
                .header("TTL", "86400")
                .header("Urgency", "high")
                .POST(HttpRequest.BodyPublishers.ofByteArray(body))
                .build();
        return http.send(request, HttpResponse.BodyHandlers.discarding()).statusCode();
    }

    static boolean allowedEndpoint(String endpoint) {
        try {
            URI uri = URI.create(endpoint);
            String host = uri.getHost() == null ? "" : uri.getHost().toLowerCase(Locale.ROOT);
            return "https".equals(uri.getScheme()) && uri.getUserInfo() == null
                    && (uri.getPort() == -1 || uri.getPort() == 443)
                    && ALLOWED_HOSTS.stream().anyMatch(allowed -> host.equals(allowed) || host.endsWith("." + allowed));
        } catch (IllegalArgumentException exception) {
            return false;
        }
    }

    private static boolean validKey(String value, int length) {
        try {
            return value != null && Base64.getUrlDecoder().decode(value).length == length;
        } catch (IllegalArgumentException exception) {
            return false;
        }
    }

    private static String json(String value) {
        return "\"" + value.replace("\\", "\\\\").replace("\"", "\\\"") + "\"";
    }
}
