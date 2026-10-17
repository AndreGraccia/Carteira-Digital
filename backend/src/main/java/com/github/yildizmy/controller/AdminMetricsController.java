package com.github.yildizmy.controller;

import com.github.yildizmy.dto.response.FutureResponses;
import com.github.yildizmy.exception.WalletRuleException;
import com.github.yildizmy.service.ForecastService;
import com.github.yildizmy.service.OpenFinanceDailySync;
import com.github.yildizmy.service.ask.AskUsage;
import lombok.RequiredArgsConstructor;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** Product numbers for the team. Aggregates only: no user, no amount of anyone in particular. Audited. */
@RestController
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
public class AdminMetricsController {

    private final ForecastService forecastService;
    private final AskUsage askUsage;
    private final OpenFinanceDailySync dailySync;

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/metrics")
    public ResponseEntity<Metrics> metrics(@RequestParam(defaultValue = "30") int days) {
        if (days < 1 || days > 365) throw new WalletRuleException("INVALID_PERIOD");
        return ResponseEntity.ok().cacheControl(CacheControl.noStore())
                .body(new Metrics(forecastService.northStar(days), askUsage.summary(days)));
    }

    /** Runs the daily sync now; for operations and tests. */
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/open-finance/daily-sync")
    public ResponseEntity<OpenFinanceDailySync.Result> runDailySync() {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(dailySync.runDaily());
    }

    /**
     * @param usage daily counts of Pergunte intents and of the simulated Pix steps (PIX_PREPARE, PIX_CONFIRM)
     */
    public record Metrics(FutureResponses.NorthStar northStar, List<AskUsage.IntentCount> usage) {
    }
}
