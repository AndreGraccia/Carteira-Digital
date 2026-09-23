package com.github.yildizmy.controller;

import com.github.yildizmy.dto.request.FutureRequests;
import com.github.yildizmy.dto.response.FutureResponses;
import com.github.yildizmy.exception.WalletRuleException;
import com.github.yildizmy.security.UserDetailsImpl;
import com.github.yildizmy.service.FutureAlertService;
import com.github.yildizmy.service.FutureService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.security.access.prepost.PreAuthorize;
import com.github.yildizmy.service.ask.AskUsage;

import java.util.List;
import java.util.Set;

/**
 * "Futuro": committed money, day-by-day projection, recurrence questions, cards/loans and
 * Modo Futuro scenarios. Read-only except for the user's own answers; nothing here moves money.
 */
@RestController
@RequestMapping("/api/v1/future")
@RequiredArgsConstructor
public class FutureController {

    private static final Set<Integer> HORIZONS = Set.of(30, 60, 90);

    private final FutureService futureService;
    private final AskUsage askUsage;
    private final FutureAlertService alertService;

    @GetMapping("/overview")
    public ResponseEntity<FutureResponses.Overview> overview(
            @AuthenticationPrincipal UserDetailsImpl user,
            @RequestParam(defaultValue = "30") int horizon) {
        if (!HORIZONS.contains(horizon)) throw new WalletRuleException("INVALID_HORIZON");
        return noStore(futureService.overview(user.getId(), horizon));
    }

    @GetMapping("/recurrences")
    public ResponseEntity<List<FutureResponses.Recurrence>> recurrences(@AuthenticationPrincipal UserDetailsImpl user) {
        return noStore(futureService.recurrences(user.getId()));
    }

    @PutMapping("/recurrences/decision")
    public ResponseEntity<Void> decide(
            @AuthenticationPrincipal UserDetailsImpl user,
            @Valid @RequestBody FutureRequests.RecurrenceDecision request) {
        futureService.decide(user.getId(), request);
        return ResponseEntity.noContent().build();
    }

    /** Read-only assistant: answers from engine numbers, never moves money. */
    @PostMapping("/ask")
    public ResponseEntity<FutureResponses.Answer> ask(
            @AuthenticationPrincipal UserDetailsImpl user,
            @Valid @RequestBody FutureRequests.Question request) {
        askUsage.acquire(user.getId());
        FutureResponses.Answer answer = futureService.ask(user.getId(), request.question());
        askUsage.record(answer.intent());
        return noStore(answer);
    }

    /** Which questions people ask, to decide when the parser needs to grow. Counts only, no text. */
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/ask/metrics")
    public ResponseEntity<List<AskUsage.IntentCount>> askMetrics(@RequestParam(defaultValue = "30") int days) {
        if (days < 1 || days > 365) throw new WalletRuleException("INVALID_PERIOD");
        return noStore(askUsage.summary(days));
    }

    @PutMapping("/daily-spending")
    public ResponseEntity<Void> decideDailySpending(
            @AuthenticationPrincipal UserDetailsImpl user,
            @Valid @RequestBody FutureRequests.DailySpendingDecision request) {
        futureService.decideDailySpending(user.getId(), request);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/alerts")
    public ResponseEntity<FutureAlertService.Settings> alerts(@AuthenticationPrincipal UserDetailsImpl user) {
        return noStore(new FutureAlertService.Settings(alertService.enabled(user.getId())));
    }

    @PutMapping("/alerts")
    public ResponseEntity<FutureAlertService.Settings> setAlerts(
            @AuthenticationPrincipal UserDetailsImpl user,
            @RequestBody FutureAlertService.Settings request) {
        alertService.setEnabled(user.getId(), request.enabled());
        return noStore(new FutureAlertService.Settings(request.enabled()));
    }

    /** Runs the daily alert job now; for operations and tests. */
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/alerts/run")
    public ResponseEntity<java.util.Map<String, Integer>> runAlerts() {
        return noStore(java.util.Map.of("sent", alertService.runDaily()));
    }

    @GetMapping("/goals")
    public ResponseEntity<List<FutureResponses.Goal>> goals(@AuthenticationPrincipal UserDetailsImpl user) {
        return noStore(futureService.goals(user.getId()));
    }

    @PostMapping("/goals")
    public ResponseEntity<FutureResponses.Goal> createGoal(
            @AuthenticationPrincipal UserDetailsImpl user,
            @Valid @RequestBody FutureRequests.Goal request) {
        return ResponseEntity.status(org.springframework.http.HttpStatus.CREATED)
                .cacheControl(CacheControl.noStore())
                .body(futureService.createGoal(user.getId(), request));
    }

    @DeleteMapping("/goals/{id}")
    public ResponseEntity<Void> deleteGoal(@AuthenticationPrincipal UserDetailsImpl user, @PathVariable long id) {
        futureService.deleteGoal(user.getId(), id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/commitments")
    public ResponseEntity<FutureResponses.Commitments> commitments(@AuthenticationPrincipal UserDetailsImpl user) {
        return noStore(futureService.commitments(user.getId()));
    }

    @PostMapping("/scenarios")
    public ResponseEntity<FutureResponses.ScenarioResult> simulate(
            @AuthenticationPrincipal UserDetailsImpl user,
            @Valid @RequestBody FutureRequests.Scenario request) {
        return noStore(futureService.simulate(user.getId(), request));
    }

    private <T> ResponseEntity<T> noStore(T body) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(body);
    }
}
