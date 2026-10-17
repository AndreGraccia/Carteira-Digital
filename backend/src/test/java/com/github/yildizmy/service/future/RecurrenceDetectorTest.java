package com.github.yildizmy.service.future;

import com.github.yildizmy.service.future.RecurrenceDetector.Category;
import com.github.yildizmy.service.future.RecurrenceDetector.Confidence;
import com.github.yildizmy.service.future.RecurrenceDetector.Direction;
import com.github.yildizmy.service.future.RecurrenceDetector.Movement;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RecurrenceDetectorTest {

    private long ids = 1;

    @Test
    void monthlyIncomeFromTheSamePayerBecomesASalaryQuestionWithEvidence() {
        var movements = List.of(
                in("2026-06-05", "2000.00", "Concentrix S.A."),
                in("2026-07-05", "2000.00", "Concentrix S.A."),
                in("2026-08-05", "2000.00", "Concentrix S.A.")
        );

        var patterns = RecurrenceDetector.detect(movements);

        assertEquals(1, patterns.size());
        var salary = patterns.get(0);
        assertEquals("in:concentrix-s-a", salary.key());
        assertEquals(Category.SALARY, salary.suggestedCategory());
        assertEquals(Confidence.HIGH, salary.confidence());
        assertEquals(5, salary.typicalDay());
        assertEquals(3, salary.occurrences().size(), "the UI shows every occurrence that led to the question");
    }

    @Test
    void smallFixedMonthlyChargeIsASubscriptionAndRentIsRecognisedByName() {
        var patterns = RecurrenceDetector.detect(List.of(
                out("2026-07-15", "39.90", "Netflix"),
                out("2026-08-15", "39.90", "NETFLIX"),
                out("2026-07-01", "1500.00", "Imobiliária Lar Seguro"),
                out("2026-08-01", "1500.00", "Imobiliaria Lar Seguro")
        ));

        assertEquals(Category.RENT, find(patterns, "out:imobiliaria-lar-seguro").suggestedCategory());
        assertEquals(Category.SUBSCRIPTION, find(patterns, "out:netflix").suggestedCategory());
        assertEquals(Confidence.MEDIUM, find(patterns, "out:netflix").confidence(), "two occurrences are not enough for HIGH");
    }

    @Test
    void weeklySpendingIsNotAMonthlyRecurrence() {
        var movements = new ArrayList<Movement>();
        for (int day = 3; day <= 27; day += 8) {
            movements.add(out("2026-07-%02d".formatted(day), "200.00", "Mercado Bairro"));
            movements.add(out("2026-08-%02d".formatted(day), "200.00", "Mercado Bairro"));
        }

        assertTrue(RecurrenceDetector.detect(movements).isEmpty());
    }

    @Test
    void sameMonthRepetitionAndUnstableAmountsAreNotRecurrences() {
        assertTrue(RecurrenceDetector.detect(List.of(
                out("2026-08-02", "50.00", "Farmácia"),
                out("2026-08-20", "50.00", "Farmácia")
        )).isEmpty(), "one month is not a pattern");

        assertTrue(RecurrenceDetector.detect(List.of(
                out("2026-07-10", "100.00", "Loja"),
                out("2026-08-10", "900.00", "Loja")
        )).isEmpty(), "amount varies too much to be the same bill");
    }

    @Test
    void moneyMovingBetweenOwnAccountsIsNeitherIncomeNorExpense() {
        var patterns = RecurrenceDetector.detect(List.of(
                out("2026-07-06", "300.00", "Transferência para reserva"),
                out("2026-08-06", "300.00", "Transferência para reserva"),
                in("2026-07-06", "300.00", "Transferência para reserva"),
                in("2026-08-06", "300.00", "Transferência para reserva")
        ));

        assertTrue(patterns.isEmpty());
    }

    @Test
    void cardBillsAndLoanInstalmentsAreLeftToCardAndLoanData() {
        var patterns = RecurrenceDetector.detect(List.of(
                out("2026-07-03", "900.00", "Pagamento fatura Aurora Card"),
                out("2026-08-03", "950.00", "Pagamento fatura Aurora Card"),
                out("2026-07-25", "612.48", "Parcela empréstimo pessoal"),
                out("2026-08-25", "612.48", "Parcela empréstimo pessoal")
        ));

        assertTrue(patterns.isEmpty(), "counting them here would double them in the projection");
    }

    @Test
    void secondaryIncomeIsNotSuggestedAsSalary() {
        var patterns = RecurrenceDetector.detect(List.of(
                in("2026-07-05", "4850.00", "Concentrix S.A."),
                in("2026-08-05", "4850.00", "Concentrix S.A."),
                in("2026-07-21", "650.00", "Marina Souza"),
                in("2026-08-21", "700.00", "Marina Souza")
        ));

        assertEquals(Category.SALARY, find(patterns, "in:concentrix-s-a").suggestedCategory());
        assertEquals(Category.OTHER_INCOME, find(patterns, "in:marina-souza").suggestedCategory());
        assertEquals(new BigDecimal("675.00"), find(patterns, "in:marina-souza").typicalAmount());
    }

    private RecurrenceDetector.RecurrencePattern find(List<RecurrenceDetector.RecurrencePattern> patterns, String key) {
        return patterns.stream().filter(pattern -> pattern.key().equals(key)).findFirst().orElseThrow();
    }

    private Movement in(String date, String amount, String counterparty) {
        return new Movement(ids++, LocalDate.parse(date), new BigDecimal(amount), Direction.IN, counterparty, "Conta", "Banco");
    }

    private Movement out(String date, String amount, String counterparty) {
        return new Movement(ids++, LocalDate.parse(date), new BigDecimal(amount), Direction.OUT, counterparty, "Conta", "Banco");
    }
}
