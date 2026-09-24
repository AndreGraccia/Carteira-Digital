package com.github.yildizmy.service;

import com.github.yildizmy.domain.entity.FutureAlert;
import com.github.yildizmy.domain.entity.User;
import com.github.yildizmy.dto.response.FutureResponses;
import com.github.yildizmy.exception.WalletRuleException;
import com.github.yildizmy.repository.FutureAlertRepository;
import com.github.yildizmy.repository.UserRepository;
import com.github.yildizmy.service.push.PushService;
import org.junit.jupiter.api.Test;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class FutureAlertServiceTest {

    private static final LocalDate START = LocalDate.of(2026, 10, 3);

    private final FutureAlertRepository alerts = mock(FutureAlertRepository.class);
    private final UserRepository users = mock(UserRepository.class);
    private final FutureService future = mock(FutureService.class);
    private final OpenFinanceEmailVerificationService verification = mock(OpenFinanceEmailVerificationService.class);
    private final JavaMailSender mail = mock(JavaMailSender.class);
    private final PushService push = mock(PushService.class);
    private final FutureAlertService service = new FutureAlertService(alerts, users, future, verification, mail, push,
            Clock.fixed(Instant.parse("2026-09-23T11:00:00Z"), ZoneOffset.UTC), "no-reply@wallia.local", "http://localhost:3000");

    @Test
    void writesOncePerWindowAndAgainOnlyAfterTheHoleIsGone() {
        assertEquals(FutureAlertService.Decision.SEND, FutureAlertService.decide(null, START));
        assertEquals(FutureAlertService.Decision.NONE, FutureAlertService.decide(START, START));
        assertEquals(FutureAlertService.Decision.SEND, FutureAlertService.decide(START, START.plusDays(30)));
        assertEquals(FutureAlertService.Decision.CLEAR, FutureAlertService.decide(START, null));
        assertEquals(FutureAlertService.Decision.NONE, FutureAlertService.decide(null, null));
    }

    @Test
    void theEmailSaysWhenHowMuchAndWhereToSeeTheWayOut() {
        String body = FutureAlertService.body("Ana", window(), limit(), "http://localhost:3000");

        assertTrue(body.contains("entre 03/10 e 04/10"));
        assertTrue(body.contains("324,87"));
        assertTrue(body.contains("Concentrix S.A., em 05/10"));
        assertTrue(body.contains("faltam R$"));
        assertTrue(body.contains("1,67"));
        assertTrue(body.contains("http://localhost:3000/future"));
    }

    @Test
    void oneUsersFailureDoesNotStopTheOthers() {
        FutureAlert broken = alert(1L);
        FutureAlert ok = alert(2L);
        when(alerts.findByEnabledTrue()).thenReturn(List.of(broken, ok));
        when(future.overview(1L, FutureAlertService.HORIZON_DAYS)).thenThrow(new IllegalStateException("boom"));
        when(future.overview(2L, FutureAlertService.HORIZON_DAYS)).thenReturn(overview());
        User user = new User();
        user.setEmail("ana@example.test");
        user.setFirstName("Ana");
        when(users.findById(2L)).thenReturn(Optional.of(user));
        when(verification.isVerified(org.mockito.ArgumentMatchers.anyLong())).thenReturn(true);

        assertEquals(1, service.runDaily());
        verify(mail, times(1)).send(any(SimpleMailMessage.class));
        assertEquals(START, ok.getLastWindowStart());
    }

    @Test
    void anAnnouncedWindowIsNotSentAgain() {
        FutureAlert alert = alert(2L);
        alert.setLastWindowStart(START);
        when(alerts.findByEnabledTrue()).thenReturn(List.of(alert));
        when(future.overview(2L, FutureAlertService.HORIZON_DAYS)).thenReturn(overview());

        assertEquals(0, service.runDaily());
        verify(mail, never()).send(any(SimpleMailMessage.class));
    }

    @Test
    void anUnverifiedEmailCannotTurnAlertsOnNorReceiveThem() {
        assertThrows(WalletRuleException.class, () -> service.setEnabled(3L, true));
        verify(alerts, never()).save(any());

        when(alerts.findByEnabledTrue()).thenReturn(List.of(alert(3L)));
        when(future.overview(3L, FutureAlertService.HORIZON_DAYS)).thenReturn(overview());
        assertEquals(0, service.runDaily());
        verify(mail, never()).send(any(SimpleMailMessage.class));
    }

    @Test
    void theGreetingKeepsOnlyOneWordOfTheName() {
        assertEquals("Olá, Ana.", FutureAlertService.greeting("  Ana Maria "));
        assertEquals("Olá, Sua.", FutureAlertService.greeting("Sua conta foi bloqueada, acesse evil.co"));
        assertEquals("Olá, httpevilco.", FutureAlertService.greeting("http://evil.co"));
        assertEquals("Olá.", FutureAlertService.greeting(null));
    }

    @Test
    void aBrowserSubscriptionAloneIsEnoughAndThePushNeverCarriesAmounts() {
        when(push.hasSubscription(4L)).thenReturn(true);
        service.setEnabled(4L, true);

        when(alerts.findByEnabledTrue()).thenReturn(List.of(alert(4L)));
        when(future.overview(4L, FutureAlertService.HORIZON_DAYS)).thenReturn(overview());
        when(push.send(org.mockito.ArgumentMatchers.eq(4L), any(), any(), any())).thenReturn(1);

        assertEquals(1, service.runDaily());
        verify(mail, never()).send(any(SimpleMailMessage.class));
        org.mockito.ArgumentCaptor<String> body = org.mockito.ArgumentCaptor.forClass(String.class);
        verify(push).send(org.mockito.ArgumentMatchers.eq(4L), any(), body.capture(), org.mockito.ArgumentMatchers.eq("/future"));
        assertEquals("A partir de 03/10. Toque para ver por quê e como sair.", body.getValue());
    }

    @Test
    void whenNoChannelDeliversTheWindowStaysUnannouncedForTomorrow() {
        FutureAlert alert = alert(5L);
        when(alerts.findByEnabledTrue()).thenReturn(List.of(alert));
        when(future.overview(5L, FutureAlertService.HORIZON_DAYS)).thenReturn(overview());
        when(push.hasSubscription(5L)).thenReturn(true);
        when(push.send(org.mockito.ArgumentMatchers.eq(5L), any(), any(), any())).thenReturn(0);

        assertEquals(0, service.runDaily());
        assertEquals(null, alert.getLastWindowStart());
    }

    private static FutureAlert alert(long userId) {
        FutureAlert alert = new FutureAlert();
        alert.setUserId(userId);
        alert.setEnabled(true);
        return alert;
    }

    private static FutureResponses.RiskWindow window() {
        FutureResponses.Event salary = new FutureResponses.Event(LocalDate.of(2026, 10, 5), new BigDecimal("4200.00"),
                "Concentrix S.A.", "INCOME", "SALARY", "ESTIMATED", "Banco Aurora", "in:concentrix");
        return new FutureResponses.RiskWindow(START, START.plusDays(1), new BigDecimal("-324.87"), START, false,
                List.of(), salary, new BigDecimal("1.67"));
    }

    private static FutureResponses.SpendingLimit limit() {
        return new FutureResponses.SpendingLimit(BigDecimal.ZERO, LocalDate.of(2026, 10, 5), true, 11, START,
                null, null, new BigDecimal("324.87"), BigDecimal.ZERO);
    }

    private static FutureResponses.Overview overview() {
        FutureResponses.Projection projection = new FutureResponses.Projection(30, BigDecimal.TEN, new BigDecimal("-324.87"),
                START, BigDecimal.TEN, 2, List.of(), List.of(window()), List.of());
        return new FutureResponses.Overview(Instant.now(), START.minusDays(10), BigDecimal.TEN, BigDecimal.TEN, null,
                projection, null, null, null, limit(), List.of(), BigDecimal.ZERO);
    }
}
