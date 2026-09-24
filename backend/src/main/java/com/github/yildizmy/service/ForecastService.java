package com.github.yildizmy.service;

import com.github.yildizmy.domain.entity.ForecastSnapshot;
import com.github.yildizmy.dto.response.FutureResponses;
import com.github.yildizmy.repository.ForecastSnapshotRepository;
import com.github.yildizmy.service.future.ForecastAccuracy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Keeps, per synced day, the real balance and the projection made from it, and grades old forecasts
 * against later balances. Recorded right after a sync, when the balance is the bank's and not a guess.
 */
@Service
public class ForecastService {

    static final int HORIZON_DAYS = ForecastAccuracy.MAX_LEAD_DAYS;
    /** Comparisons shown on screen; the averages use every day in the window. */
    static final int RECENT = 10;
    // ponytail: rows older than this are ignored, not deleted. Add a cleanup job when the table matters.
    static final int WINDOW_DAYS = 120;

    private final ForecastSnapshotRepository repository;
    private final FutureService futureService;
    private final Clock clock;

    public ForecastService(ForecastSnapshotRepository repository, FutureService futureService, Clock clock) {
        this.repository = repository;
        this.futureService = futureService;
        this.clock = clock;
    }

    /** Upsert of today's row: syncing twice a day keeps the latest balance and forecast. */
    @Transactional
    public void record(long userId) {
        FutureResponses.Overview overview = futureService.overview(userId, HORIZON_DAYS);
        ForecastSnapshot snapshot = repository.findByUserIdAndTakenOn(userId, overview.today()).orElseGet(() -> {
            ForecastSnapshot created = new ForecastSnapshot();
            created.setUserId(userId);
            created.setTakenOn(overview.today());
            return created;
        });
        snapshot.setBalance(overview.balance());
        snapshot.setProjection(overview.sources().confirmedRecurrences() == 0 ? null
                : overview.projection().days().stream()
                        .map(day -> day.balance().toPlainString())
                        .collect(Collectors.joining(";")));
        snapshot.setUpdatedAt(clock.instant());
        repository.save(snapshot);
    }

    @Transactional(readOnly = true)
    public FutureResponses.Accuracy accuracy(long userId) {
        LocalDate from = LocalDate.now(clock.withZone(FutureService.ZONE)).minusDays(WINDOW_DAYS);
        List<ForecastAccuracy.Snapshot> snapshots = repository.findByUserIdAndTakenOnGreaterThanEqualOrderByTakenOn(userId, from)
                .stream()
                .map(row -> new ForecastAccuracy.Snapshot(row.getTakenOn(), row.getBalance(), parse(row.getProjection())))
                .toList();
        ForecastAccuracy.Result result = ForecastAccuracy.compare(snapshots);
        List<ForecastAccuracy.Comparison> all = result.comparisons();
        return new FutureResponses.Accuracy(
                snapshots.isEmpty() ? null : snapshots.get(0).takenOn(),
                result.firstComparisonOn(),
                all.size(),
                result.meanAbsoluteError(),
                result.bias(),
                result.avoided(),
                result.missed(),
                all.subList(Math.max(0, all.size() - RECENT), all.size()).stream()
                        .map(item -> new FutureResponses.AccuracyPoint(item.date(), item.forecastOn(), item.projected(),
                                item.actual(), item.error()))
                        .toList());
    }

    static List<BigDecimal> parse(String projection) {
        if (projection == null || projection.isBlank()) return List.of();
        return Arrays.stream(projection.split(";")).map(BigDecimal::new).toList();
    }
}
