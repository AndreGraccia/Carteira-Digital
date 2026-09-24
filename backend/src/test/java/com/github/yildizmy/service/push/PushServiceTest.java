package com.github.yildizmy.service.push;

import com.github.yildizmy.domain.entity.PushSubscription;
import com.github.yildizmy.exception.WalletRuleException;
import com.github.yildizmy.repository.PushSubscriptionRepository;
import com.github.yildizmy.service.UserService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Base64;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PushServiceTest {

    private static final String[] VAPID = WebPush.newVapidKeys();
    private static final String P256DH = WebPush.newVapidKeys()[0];
    private static final String AUTH = Base64.getUrlEncoder().withoutPadding().encodeToString(new byte[16]);
    private static final String FCM = "https://fcm.googleapis.com/fcm/send/abc";

    private final PushSubscriptionRepository repository = mock(PushSubscriptionRepository.class);
    private final Clock clock = Clock.fixed(Instant.parse("2026-09-24T11:00:00Z"), ZoneOffset.UTC);

    @ParameterizedTest
    @ValueSource(strings = {
            "https://fcm.googleapis.com/fcm/send/x",
            "https://updates.push.services.mozilla.com/wpush/v2/x",
            "https://web.push.apple.com/x",
            "https://wns2-par02p.notify.windows.com/w/?token=x"})
    void theBrowsersPushServicesAreAccepted(String endpoint) {
        assertTrue(PushService.allowedEndpoint(endpoint));
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "http://fcm.googleapis.com/fcm/send/x",
            "https://169.254.169.254/latest/meta-data",
            "https://localhost:8082/api/v1/admin/metrics",
            "https://fcm.googleapis.com.evil.io/x",
            "https://evilfcm.googleapis.com.io/x",
            "https://user@fcm.googleapis.com/x",
            "https://fcm.googleapis.com:8443/x",
            "not a url"})
    void anyOtherAddressIsRefusedSoTheApiCannotBeAimedAtInternalUrls(String endpoint) {
        assertFalse(PushService.allowedEndpoint(endpoint));
    }

    @Test
    void withoutVapidKeysPushIsOffAndSubscribingIsRefused() {
        PushService off = new PushService(repository, mock(UserService.class), clock, "", "", "mailto:x@y.z");

        assertNull(off.publicKey());
        assertThrows(WalletRuleException.class, () -> off.subscribe(1L, FCM, P256DH, AUTH, false));
        assertEquals(0, off.send(1L, "t", "b", "/future"));
    }

    @Test
    void fiveBrowsersIsTheLimitAndBadKeysAreRefused() {
        PushService service = service(200);
        when(repository.findByEndpoint(FCM)).thenReturn(Optional.empty());
        when(repository.countByUserId(1L)).thenReturn(5L);

        assertThrows(WalletRuleException.class, () -> service.subscribe(1L, FCM, P256DH, AUTH, false));
        assertThrows(WalletRuleException.class, () -> service.subscribe(1L, FCM, "short", AUTH, false));
        verify(repository, never()).save(any());
    }

    @Test
    void aBrowserOfAnotherAccountIsNeverTakenOnPageLoadAndMovingItCountsAgainstTheLimit() {
        PushService service = service(201);
        PushSubscription previousOwner = subscription(FCM);
        previousOwner.setUserId(2L);
        when(repository.findByEndpoint(FCM)).thenReturn(Optional.of(previousOwner));

        assertFalse(service.subscribe(1L, FCM, P256DH, AUTH, true));
        assertEquals(2L, previousOwner.getUserId());

        when(repository.countByUserId(1L)).thenReturn(5L);
        assertThrows(WalletRuleException.class, () -> service.subscribe(1L, FCM, P256DH, AUTH, false));

        when(repository.countByUserId(1L)).thenReturn(0L);
        assertTrue(service.subscribe(1L, FCM, P256DH, AUTH, false));
        assertEquals(1L, previousOwner.getUserId());
    }

    @Test
    void aBrowserThatUninstalledIsForgottenAndOnlyAcceptedPushesCount() {
        PushSubscription gone = subscription(FCM);
        PushSubscription ok = subscription("https://updates.push.services.mozilla.com/wpush/v2/y");
        when(repository.findByUserId(1L)).thenReturn(List.of(gone, ok));
        PushService service = new PushService(repository, mock(UserService.class), clock, VAPID[0], VAPID[1], "mailto:x@y.z") {
            @Override
            int post(PushSubscription subscription, byte[] body) {
                return subscription == gone ? 410 : 201;
            }
        };

        assertEquals(1, service.send(1L, "Wall.ia", "A partir de 03/10.", "/future"));
        verify(repository).deleteById(gone.getId());
        verify(repository, never()).deleteById(ok.getId());
    }

    private PushService service(int status) {
        return new PushService(repository, mock(UserService.class), clock, VAPID[0], VAPID[1], "mailto:x@y.z") {
            @Override
            int post(PushSubscription subscription, byte[] body) {
                return status;
            }
        };
    }

    private static PushSubscription subscription(String endpoint) {
        PushSubscription subscription = new PushSubscription();
        subscription.setId((long) endpoint.hashCode());
        subscription.setUserId(1L);
        subscription.setEndpoint(endpoint);
        subscription.setP256dh(P256DH);
        subscription.setAuth(AUTH);
        return subscription;
    }
}
