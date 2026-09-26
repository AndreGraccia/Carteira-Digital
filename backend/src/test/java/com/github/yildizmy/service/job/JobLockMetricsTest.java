package com.github.yildizmy.service.job;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JobLockMetricsTest {

    private final SimpleMeterRegistry registry = new SimpleMeterRegistry();
    private final JobLock lock = JobLock.inProcess(registry);

    @Test
    void aSuccessfulRunIsTimedAndMarksTheLastSuccess() {
        lock.runExclusive("daily-sync", () -> 1);

        assertEquals(1, registry.get("wallia.job").tag("name", "daily-sync").tag("outcome", "success").timer().count());
        assertTrue(registry.get("wallia.job.last.success").tag("name", "daily-sync").gauge().value() > 0,
                "an alert can fire when this stops moving");
    }

    @Test
    void aFailingRunIsCountedAsFailureAndStillThrows() {
        assertThrows(IllegalStateException.class, () -> lock.runExclusive("retention", () -> {
            throw new IllegalStateException("database down");
        }));

        assertEquals(1, registry.get("wallia.job").tag("name", "retention").tag("outcome", "failure").timer().count());
        assertEquals(0.0, registry.get("wallia.job.last.success").tag("name", "retention").gauge().value(),
                "no success yet");
    }
}
