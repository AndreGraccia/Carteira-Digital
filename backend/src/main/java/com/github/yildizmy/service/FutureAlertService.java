package com.github.yildizmy.service;

import com.github.yildizmy.domain.entity.FutureAlert;
import com.github.yildizmy.domain.entity.User;
import com.github.yildizmy.dto.response.FutureResponses;
import com.github.yildizmy.exception.WalletRuleException;
import com.github.yildizmy.repository.FutureAlertRepository;
import com.github.yildizmy.repository.UserRepository;
import com.github.yildizmy.service.push.PushService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.NumberFormat;
import java.time.Clock;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/**
 * Opt-in e-mail when the projection shows a new negative window. The daily job reuses the same engine
 * as the Future page, writes once per window, and never includes more than the dates and amounts.
 */
@Slf4j
@Service
public class FutureAlertService {

    static final int HORIZON_DAYS = 30;
    private static final Locale PT_BR = Locale.forLanguageTag("pt-BR");
    private static final DateTimeFormatter DAY = DateTimeFormatter.ofPattern("dd/MM");

    private final FutureAlertRepository alertRepository;
    private final UserRepository userRepository;
    private final FutureService futureService;
    private final OpenFinanceEmailVerificationService emailVerification;
    private final JavaMailSender mailSender;
    private final PushService pushService;
    private final Clock clock;
    private final String from;
    private final String appUrl;

    public FutureAlertService(
            FutureAlertRepository alertRepository,
            UserRepository userRepository,
            FutureService futureService,
            OpenFinanceEmailVerificationService emailVerification,
            JavaMailSender mailSender,
            PushService pushService,
            Clock clock,
            @Value("${app.open-finance.verification.mail-from}") String from,
            @Value("${app.security.allowedOrigin}") String appUrl) {
        this.alertRepository = alertRepository;
        this.userRepository = userRepository;
        this.futureService = futureService;
        this.emailVerification = emailVerification;
        this.mailSender = mailSender;
        this.pushService = pushService;
        this.clock = clock;
        this.from = from;
        this.appUrl = appUrl;
    }

    @Transactional(readOnly = true)
    public boolean enabled(long userId) {
        return alertRepository.findById(userId).map(FutureAlert::isEnabled).orElse(false);
    }

    @Transactional
    public void setEnabled(long userId, boolean enabled) {
        // Sign-up does not confirm the e-mail; without this, anyone could aim Wall.ia's mail at a stranger.
        // A browser subscribed from the user's own session is a channel of its own.
        if (enabled && !emailVerification.isVerified(userId) && !pushService.hasSubscription(userId)) {
            throw new WalletRuleException("ALERT_EMAIL_NOT_VERIFIED");
        }
        FutureAlert alert = alertRepository.findById(userId).orElseGet(() -> {
            FutureAlert created = new FutureAlert();
            created.setUserId(userId);
            return created;
        });
        alert.setEnabled(enabled);
        if (!enabled) alert.setLastWindowStart(null);
        alert.setUpdatedAt(clock.instant());
        alertRepository.save(alert);
    }

    /** Every morning in Brasília time; "-" in {@code future_alert_cron} turns it off. */
    @Scheduled(cron = "${app.future.alert-cron:0 0 8 * * *}", zone = "America/Sao_Paulo")
    public int runDaily() {
        int sent = 0;
        for (FutureAlert alert : alertRepository.findByEnabledTrue()) {
            try {
                if (check(alert)) sent++;
            } catch (RuntimeException exception) {
                // One user's failure (mail down, bad data) must not stop the others.
                log.warn("Future alert skipped for one user: {}", exception.getClass().getSimpleName());
            }
        }
        return sent;
    }

    private boolean check(FutureAlert alert) {
        // The verification lasts 90 days or ends when the e-mail changes; without it, only push is left.
        boolean email = emailVerification.isVerified(alert.getUserId());
        boolean push = pushService.hasSubscription(alert.getUserId());
        if (!email && !push) return false;
        FutureResponses.Overview overview = futureService.overview(alert.getUserId(), HORIZON_DAYS);
        FutureResponses.RiskWindow window = overview.projection().riskWindows().stream().findFirst().orElse(null);
        Decision decision = decide(alert.getLastWindowStart(), window == null ? null : window.start());
        if (decision == Decision.NONE) return false;
        if (decision == Decision.CLEAR) {
            alert.setLastWindowStart(null);
            alertRepository.save(alert);
            return false;
        }
        boolean delivered = false;
        if (email) {
            try {
                send(userRepository.findById(alert.getUserId()).orElseThrow(), window, overview.spendingLimit());
                delivered = true;
            } catch (IllegalStateException exception) {
                log.warn("Alert e-mail failed for one user; trying push");
            }
        }
        // Lock-screen text: the date only, never amounts.
        if (push && pushService.send(alert.getUserId(), "Wall.ia: seu saldo pode ficar negativo",
                "A partir de %s. Toque para ver por quê e como sair.".formatted(window.start().format(DAY)), "/future") > 0) {
            delivered = true;
        }
        // Nothing got through: keep the window unannounced so tomorrow tries again.
        if (!delivered) return false;
        alert.setLastWindowStart(window.start());
        alert.setLastSentAt(clock.instant());
        alertRepository.save(alert);
        return true;
    }

    /** SEND for a window not announced yet, CLEAR when the hole is gone (so a future one is announced), NONE otherwise. */
    static Decision decide(LocalDate lastAnnounced, LocalDate currentWindowStart) {
        if (currentWindowStart == null) return lastAnnounced == null ? Decision.NONE : Decision.CLEAR;
        return currentWindowStart.equals(lastAnnounced) ? Decision.NONE : Decision.SEND;
    }

    static String body(String firstName, FutureResponses.RiskWindow window, FutureResponses.SpendingLimit limit, String url) {
        NumberFormat currency = NumberFormat.getCurrencyInstance(PT_BR);
        String cost = window.overdraftCostCap() != null && window.overdraftCostCap().signum() > 0
                ? "\nSe o banco cobrir com cheque especial, os juros podem chegar a %s (teto legal de 8%% ao mês)."
                        .formatted(currency.format(window.overdraftCostCap()))
                : "";
        String relief = window.relief() == null ? ""
                : "\nA próxima entrada prevista é %s, em %s.".formatted(window.relief().label(), window.relief().date().format(DAY));
        String gap = limit != null && limit.shortfall() != null && limit.shortfall().signum() > 0
                ? "\nMesmo sem gastar nada no dia a dia, faltam %s até a próxima entrada.".formatted(currency.format(limit.shortfall()))
                : "";
        return """
                %s

                Pela projeção da Wall.ia, seu saldo pode ficar abaixo de zero entre %s e %s, chegando a %s.%s%s%s

                Veja por quê e as opções calculadas para sair do buraco:
                %s/future

                Você recebe este aviso porque ativou os alertas na tela Futuro. Para parar, desative lá.
                A Wall.ia nunca pede senha, código ou dados bancários por e-mail.
                """.formatted(
                greeting(firstName),
                window.start().format(DAY),
                window.end().format(DAY),
                currency.format(window.lowestBalance()),
                relief, gap, cost,
                url);
    }

    /** The name is free text typed at sign-up; keep one word of letters so it cannot carry a phishing line. */
    static String greeting(String firstName) {
        String word = firstName == null ? "" : firstName.strip().split("\\s+")[0].replaceAll("[^\\p{L}'-]", "");
        if (word.length() > 30) word = word.substring(0, 30);
        return word.isEmpty() ? "Olá." : "Olá, " + word + ".";
    }

    private void send(User user, FutureResponses.RiskWindow window, FutureResponses.SpendingLimit limit) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(from);
        message.setTo(user.getEmail());
        message.setSubject("Wall.ia: seu saldo pode ficar negativo a partir de " + window.start().format(DAY));
        message.setText(body(user.getFirstName(), window, limit, appUrl));
        try {
            mailSender.send(message);
        } catch (MailException exception) {
            throw new IllegalStateException("mail unavailable", exception);
        }
    }

    enum Decision { SEND, CLEAR, NONE }

    /** Body of GET and PUT /future/alerts. */
    public record Settings(boolean enabled) {
    }
}
