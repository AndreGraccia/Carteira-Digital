package com.github.yildizmy.service;

import com.github.yildizmy.service.future.CashEvent;
import com.github.yildizmy.service.future.CashEvent.Certainty;
import com.github.yildizmy.service.future.CashEvent.Kind;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class FutureScheduledTest {

    private static final LocalDate DAY = LocalDate.of(2026, 10, 1);

    @Test
    void theBankScheduleReplacesTheEstimateForTheSameBill() {
        var estimatedRent = event(DAY.plusDays(2), "-1500.00", "Imobiliária Lar Seguro", Certainty.ESTIMATED);
        var estimatedGym = event(DAY.plusDays(9), "-119.90", "Smart Fit", Certainty.ESTIMATED);
        var invoice = event(DAY.plusDays(1), "-800.00", "Imobiliaria Lar Seguro", Certainty.CONFIRMED);
        var scheduledRent = event(DAY, "-1500.00", "IMOBILIÁRIA LAR SEGURO", Certainty.CONFIRMED);

        var events = FutureService.withScheduled(List.of(estimatedRent, estimatedGym, invoice), List.of(scheduledRent));

        assertEquals(List.of(scheduledRent, estimatedGym, invoice), events,
                "the estimate goes, a confirmed event with a similar name stays, other bills stay");
    }

    @Test
    void aScheduleFarFromTheEstimateIsAnotherPayment() {
        var estimatedRent = event(DAY.plusDays(20), "-1500.00", "Imobiliária", Certainty.ESTIMATED);
        var scheduled = event(DAY, "-1500.00", "Imobiliária", Certainty.CONFIRMED);

        assertEquals(2, FutureService.withScheduled(List.of(estimatedRent), List.of(scheduled)).size());
    }

    private static CashEvent event(LocalDate date, String amount, String label, Certainty certainty) {
        return new CashEvent(date, new BigDecimal(amount), label, Kind.RECURRING_EXPENSE, "BILL", certainty, "Banco", label);
    }
}
