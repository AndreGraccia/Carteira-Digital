package com.github.yildizmy.service.openfinance;

import com.github.yildizmy.service.future.CardPlan;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Local contract sandbox: a deterministic customer shaped like the official Accounts, Credit Card
 * and Loans APIs. It never talks to a real institution.
 * <p>
 * The history is built on calendar days (salary on the 5th, rent on the 1st…) for the last three
 * months up to today, and the account balance is the result of replaying that history. That keeps
 * balance, statement, invoices and projections consistent with each other on any date.
 */
@Component
@ConditionalOnProperty(name = "app.open-finance.mode", havingValue = "contract-sandbox", matchIfMissing = true)
public class ContractSandboxOpenFinanceGateway implements OpenFinanceGateway {

    static final ZoneId ZONE = ZoneId.of("America/Sao_Paulo");
    static final String INSTITUTION = "Banco Aurora Sandbox";
    private static final BigDecimal CHECKING_OPENING = new BigDecimal("350.00");
    private static final BigDecimal SAVINGS_OPENING = new BigDecimal("400.00");
    private static final int HISTORY_MONTHS = 3;
    private static final CardPlan CARD_CALENDAR = new CardPlan(0, "", "", 25, 2, List.of());

    private final Clock clock;

    public ContractSandboxOpenFinanceGateway(Clock clock) {
        this.clock = clock;
    }

    @Override
    public OpenFinanceSnapshot fetch(long userId) {
        Instant now = clock.instant();
        LocalDate today = LocalDate.ofInstant(now, ZONE);
        YearMonth current = YearMonth.from(today);
        YearMonth first = current.minusMonths(HISTORY_MONTHS);

        List<OpenFinanceSnapshot.CardTransaction> cardTransactions = cardTransactions(first, current, today);
        List<OpenFinanceSnapshot.Entry> checking = new ArrayList<>();
        List<OpenFinanceSnapshot.Entry> savings = new ArrayList<>();

        String[] energy = {"176.40", "203.10", "189.75", "214.30"};
        String[][] market = {{"231.80", "187.45", "249.10", "205.60"}, {"198.30", "262.75", "214.90", "179.35"},
                {"244.60", "201.15", "226.40", "190.85"}, {"215.25", "238.90", "183.70", "257.40"}};
        int index = 0;
        for (YearMonth month = first; !month.isAfter(current); month = month.plusMonths(1), index++) {
            String m = month.toString();
            add(checking, today, month, 1, "rent-" + m, "DEBITO", "Imobiliária Lar Seguro", "PIX", "1500.00");
            BigDecimal invoice = invoiceTotal(cardTransactions, month);
            if (invoice.signum() > 0) {
                add(checking, today, month, 2, "card-bill-" + m, "DEBITO", "Pagamento fatura Aurora Card", "BOLETO",
                        invoice.toPlainString());
            }
            add(checking, today, month, 5, "salary-" + m, "CREDITO", "Concentrix S.A.", "TED", "4200.00");
            add(checking, today, month, 6, "reserve-out-" + m, "DEBITO", "Transferência para reserva",
                    "TRANSFERENCIA_MESMA_INSTITUICAO", "300.00");
            add(savings, today, month, 6, "reserve-in-" + m, "CREDITO", "Transferência para reserva",
                    "TRANSFERENCIA_MESMA_INSTITUICAO", "300.00");
            add(checking, today, month, 8, "school-" + m, "DEBITO", "Fluency Escola de Inglês", "BOLETO", "389.00");
            add(checking, today, month, 10, "gym-" + m, "DEBITO", "Smart Fit", "DEBITO_AUTOMATICO", "119.90");
            add(checking, today, month, 12, "internet-" + m, "DEBITO", "Vivo Fibra", "DEBITO_AUTOMATICO", "119.90");
            add(checking, today, month, 15, "streaming-" + m, "DEBITO", "Netflix", "DEBITO_AUTOMATICO", "39.90");
            add(checking, today, month, 18, "energy-" + m, "DEBITO", "Enel Distribuição SP", "BOLETO", energy[index % 4]);
            add(checking, today, month, 25, "loan-" + m, "DEBITO", "Parcela empréstimo pessoal", "DEBITO_AUTOMATICO", "612.48");
            int[] marketDays = {3, 11, 19, 27};
            for (int i = 0; i < marketDays.length; i++) {
                add(checking, today, month, marketDays[i], "market-" + m + "-" + i, "DEBITO", "Mercado Bairro",
                        "COMPRA_DEBITO", market[index % 4][i]);
            }
            if (index == 1 || index == 2) {
                add(checking, today, month, 21, "freela-" + m, "CREDITO", "Marina Souza", "PIX",
                        index == 1 ? "650.00" : "700.00");
            }
        }

        return new OpenFinanceSnapshot(
                "sandbox-21128159000166",
                INSTITUTION,
                UUID.nameUUIDFromBytes(("ofb-consent-" + userId).getBytes(StandardCharsets.UTF_8)).toString(),
                now,
                now.plus(90, ChronoUnit.DAYS),
                List.of(
                        new OpenFinanceSnapshot.Account(
                                "acc-checking-demo", INSTITUTION, "CONTA_DEPOSITO_A_VISTA",
                                "0001", "94088392", "4", replay(CHECKING_OPENING, checking), checking),
                        new OpenFinanceSnapshot.Account(
                                "acc-savings-demo", INSTITUTION, "CONTA_POUPANCA",
                                "0001", "24550245", "7", replay(SAVINGS_OPENING, savings), savings)
                ),
                List.of(new OpenFinanceSnapshot.CreditCardAccount(
                        "card-platinum-demo", "Aurora Card Platinum", "4821", new BigDecimal("8000.00"),
                        CARD_CALENDAR.closingDay(), CARD_CALENDAR.dueDay(), cardTransactions)),
                List.of(loan(today))
        );
    }

    private List<OpenFinanceSnapshot.CardTransaction> cardTransactions(YearMonth first, YearMonth current, LocalDate today) {
        List<OpenFinanceSnapshot.CardTransaction> items = new ArrayList<>();
        // Instalment purchases that still weigh on the next invoices.
        card(items, today, first.plusMonths(1).atDay(14), "tv", "Smart TV 55\" · Magazine Aurora", "3299.00", 10);
        card(items, today, current.minusMonths(1).atDay(2), "flight", "Passagem aérea · Voa Brasil", "1287.00", 6);
        card(items, today, current.minusMonths(1).atDay(20), "sneakers", "Tênis de corrida", "399.90", 3);
        for (YearMonth month = first; !month.isAfter(current); month = month.plusMonths(1)) {
            card(items, today, month.atDay(9), "spotify-" + month, "Spotify", "21.90", 1);
            card(items, today, month.atDay(16), "food-" + month, "iFood", "68.40", 1);
            card(items, today, month.atDay(23), "fuel-" + month, "Posto Shell", "180.00", 1);
        }
        return items;
    }

    private void card(List<OpenFinanceSnapshot.CardTransaction> items, LocalDate today, LocalDate date,
                      String id, String name, String amount, int installments) {
        if (!date.isBefore(today)) return;
        items.add(new OpenFinanceSnapshot.CardTransaction(
                id, name, date, new BigDecimal(amount), installments, CARD_CALENDAR.invoiceFor(date).atDay(1)));
    }

    private BigDecimal invoiceTotal(List<OpenFinanceSnapshot.CardTransaction> items, YearMonth invoice) {
        return items.stream()
                .map(item -> {
                    long offset = ChronoUnit.MONTHS.between(YearMonth.from(item.firstInvoiceDue()), invoice);
                    if (offset < 0 || offset >= item.installments()) return BigDecimal.ZERO;
                    return item.amount().divide(BigDecimal.valueOf(item.installments()), 2, RoundingMode.DOWN);
                })
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private OpenFinanceSnapshot.LoanContract loan(LocalDate today) {
        LocalDate thisMonthDue = today.withDayOfMonth(25);
        LocalDate nextDue = thisMonthDue.isBefore(today) ? thisMonthDue.plusMonths(1) : thisMonthDue;
        int paid = 9 + (thisMonthDue.isBefore(today) ? 1 : 0);
        int total = 24;
        BigDecimal installment = new BigDecimal("612.48");
        // ponytail: outstanding approximated as remaining instalments at ~2.2% a.m. present value.
        BigDecimal outstanding = presentValue(installment, total - paid, new BigDecimal("0.022"));
        return new OpenFinanceSnapshot.LoanContract(
                "loan-personal-demo", "Empréstimo pessoal", new BigDecimal("12000.00"), outstanding,
                installment, total, paid, nextDue, new BigDecimal("29.8400"), 25, "PRICE");
    }

    private BigDecimal presentValue(BigDecimal installment, int remaining, BigDecimal monthlyRate) {
        BigDecimal value = BigDecimal.ZERO;
        BigDecimal factor = BigDecimal.ONE.add(monthlyRate);
        BigDecimal discount = BigDecimal.ONE;
        for (int i = 0; i < remaining; i++) {
            discount = discount.multiply(factor);
            value = value.add(installment.divide(discount, 10, RoundingMode.HALF_EVEN));
        }
        return value.setScale(2, RoundingMode.HALF_EVEN);
    }

    private void add(List<OpenFinanceSnapshot.Entry> entries, LocalDate today, YearMonth month, int day,
                     String id, String direction, String name, String type, String amount) {
        LocalDate date = month.atDay(Math.min(day, month.lengthOfMonth()));
        if (!date.isBefore(today)) return;
        entries.add(new OpenFinanceSnapshot.Entry(id, direction, name, type, new BigDecimal(amount),
                date.atTime(LocalTime.of(10, 0)).atZone(ZONE).toInstant()));
    }

    private BigDecimal replay(BigDecimal opening, List<OpenFinanceSnapshot.Entry> entries) {
        return entries.stream()
                .map(entry -> "CREDITO".equals(entry.creditDebitType()) ? entry.amount() : entry.amount().negate())
                .reduce(opening, BigDecimal::add);
    }

    @Override
    public String mode() {
        return "CONTRACT_SANDBOX";
    }

    @Override
    public boolean simulated() {
        return true;
    }
}
