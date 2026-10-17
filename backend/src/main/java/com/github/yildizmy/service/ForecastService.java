package com.github.yildizmy.service;

import com.github.yildizmy.domain.entity.ForecastSnapshot;
import com.github.yildizmy.dto.response.FutureResponses;
import com.github.yildizmy.repository.ForecastSnapshotRepository;
import com.github.yildizmy.service.future.ForecastAccuracy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
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

    /**
     * The north star for the team: negative days forecast and avoided, per active user per month, over the
     * last {@code days}. Active = synced at least once in the period. Misses are reported next to it.
     */
    // ponytail: loads the period for every user and grades in memory. Move to a nightly rollup table when
    // the snapshot count makes this endpoint slow.
    @Transactional(readOnly = true)
    public FutureResponses.NorthStar northStar(int days) {
        LocalDate today = LocalDate.now(clock.withZone(FutureService.ZONE));
        LocalDate periodStart = today.minusDays(days - 1L);
        Map<Long, List<ForecastAccuracy.Snapshot>> byUser = new LinkedHashMap<>();
        for (ForecastSnapshot row : repository.findByTakenOnGreaterThanEqualOrderByUserIdAscTakenOnAsc(
                periodStart.minusDays(ForecastAccuracy.MAX_LEAD_DAYS))) {
            byUser.computeIfAbsent(row.getUserId(), ignored -> new ArrayList<>())
                    .add(new ForecastAccuracy.Snapshot(row.getTakenOn(), row.getBalance(), parse(row.getProjection())));
        }
        int active = 0;
        int graded = 0;
        int avoided = 0;
        int missed = 0;
        List<BigDecimal> errors = new ArrayList<>();
        for (List<ForecastAccuracy.Snapshot> snapshots : byUser.values()) {
            if (snapshots.stream().anyMatch(item -> !item.takenOn().isBefore(periodStart))) active++;
            List<ForecastAccuracy.Comparison> inPeriod = ForecastAccuracy.compare(snapshots).comparisons().stream()
                    .filter(item -> !item.date().isBefore(periodStart))
                    .toList();
            if (inPeriod.isEmpty()) continue;
            graded++;
            for (ForecastAccuracy.Comparison item : inPeriod) {
                errors.add(item.error().abs());
                if (item.projected().signum() < 0 && item.actual().signum() >= 0) avoided++;
                if (item.projected().signum() >= 0 && item.actual().signum() < 0) missed++;
            }
        }
        BigDecimal mean = errors.isEmpty() ? null : errors.stream().reduce(BigDecimal.ZERO, BigDecimal::add)
                .divide(BigDecimal.valueOf(errors.size()), 2, RoundingMode.HALF_UP);
        BigDecimal perUserMonth = active == 0 ? null : BigDecimal.valueOf(avoided)
                .multiply(BigDecimal.valueOf(30))
                .divide(BigDecimal.valueOf((long) active * days), 2, RoundingMode.HALF_UP);
        return new FutureResponses.NorthStar(days, active, graded, errors.size(), avoided, missed, mean, perUserMonth);
    }

    static List<BigDecimal> parse(String projection) {
        if (projection == null || projection.isBlank()) return List.of();
        return Arrays.stream(projection.split(";")).map(BigDecimal::new).toList();
    }
}
