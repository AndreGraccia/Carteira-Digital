package com.github.yildizmy.service;

import com.github.yildizmy.domain.enums.OpenFinanceConnectionStatus;
import com.github.yildizmy.repository.OpenFinanceConnectionRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

/**
 * Syncs every authorised connection once a day and records the forecast, so the accuracy score and the
 * 8h alerts read today's balance instead of whatever the user last synced by hand.
 * <p>
 * Each user goes through {@link OpenFinanceService#sync}, which commits on its own and re-checks the
 * consent; an expired one is marked REJECTED (CONSENT_MAX_DATE_REACHED) and skipped, never renewed here.
 */
@Slf4j
@Service
public class OpenFinanceDailySync {

    private final OpenFinanceConnectionRepository connectionRepository;
    private final OpenFinanceService openFinanceService;
    private final ForecastService forecastService;

    public OpenFinanceDailySync(OpenFinanceConnectionRepository connectionRepository,
                                OpenFinanceService openFinanceService,
                                ForecastService forecastService) {
        this.connectionRepository = connectionRepository;
        this.openFinanceService = openFinanceService;
        this.forecastService = forecastService;
    }

    // ponytail: one user after another on the scheduler thread. Parallelise or queue when a run takes minutes.
    @Scheduled(cron = "${app.open-finance.daily-sync-cron:0 30 7 * * *}", zone = "America/Sao_Paulo")
    public Result runDaily() {
        int synced = 0;
        int skipped = 0;
        for (Long userId : connectionRepository.findUserIdsByStatus(OpenFinanceConnectionStatus.AUTHORISED)) {
            try {
                openFinanceService.sync(userId);
            } catch (RuntimeException exception) {
                // Expired consent or the bank being down: this user waits for tomorrow.
                skipped++;
                log.warn("Daily sync skipped for one user: {}", exception.getClass().getSimpleName());
                continue;
            }
            synced++;
            try {
                forecastService.record(userId);
            } catch (RuntimeException exception) {
                log.warn("Forecast snapshot skipped for one user: {}", exception.getClass().getSimpleName());
            }
        }
        log.info("Daily Open Finance sync: {} synced, {} skipped", synced, skipped);
        return new Result(synced, skipped);
    }

    public record Result(int synced, int skipped) {
    }
}
