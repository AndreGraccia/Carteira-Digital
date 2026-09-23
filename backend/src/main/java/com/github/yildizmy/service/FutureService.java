package com.github.yildizmy.service;

import com.github.yildizmy.domain.entity.CreditCard;
import com.github.yildizmy.domain.entity.Loan;
import com.github.yildizmy.domain.entity.RecurrenceDecision;
import com.github.yildizmy.domain.entity.Transaction;
import com.github.yildizmy.domain.entity.Wallet;
import com.github.yildizmy.domain.enums.RecurrenceStatus;
import com.github.yildizmy.domain.enums.Status;
import com.github.yildizmy.domain.enums.TransactionSource;
import com.github.yildizmy.dto.request.FutureRequests;
import com.github.yildizmy.dto.response.FutureResponses;
import com.github.yildizmy.exception.WalletRuleException;
import com.github.yildizmy.repository.CreditCardRepository;
import com.github.yildizmy.repository.LoanRepository;
import com.github.yildizmy.repository.RecurrenceDecisionRepository;
import com.github.yildizmy.repository.TransactionRepository;
import com.github.yildizmy.repository.WalletRepository;
import com.github.yildizmy.service.future.CardPlan;
import com.github.yildizmy.service.future.CashEvent;
import com.github.yildizmy.service.future.CashEventPlanner;
import com.github.yildizmy.service.future.CashEventPlanner.ConfirmedRecurrence;
import com.github.yildizmy.service.future.LoanPlan;
import com.github.yildizmy.service.future.ProjectionEngine;
import com.github.yildizmy.service.future.RecurrenceDetector;
import com.github.yildizmy.service.future.RecurrenceDetector.Category;
import com.github.yildizmy.service.future.RecurrenceDetector.Direction;
import com.github.yildizmy.service.future.RecurrenceDetector.Movement;
import com.github.yildizmy.service.future.RecurrenceDetector.RecurrencePattern;
import com.github.yildizmy.service.future.ScenarioSimulator;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Application service for "Futuro": loads the user's data, runs the deterministic engine and
 * maps the result. Java calculates here; the AI layer may only read these results.
 */
@Service
public class FutureService {

    static final ZoneId ZONE = ZoneId.of("America/Sao_Paulo");
    static final int COMMITTED_WINDOW_DAYS = 30;
    static final int SCENARIO_HORIZON_DAYS = 90;
    private static final long TYPE_DEPOSIT = 4L;
    private static final long TYPE_WITHDRAWAL = 5L;
    private static final Set<String> INCOME_CATEGORIES = Set.of("SALARY", "OTHER_INCOME");
    private static final Set<String> EXPENSE_CATEGORIES = Set.of("RENT", "SUBSCRIPTION", "BILL", "OTHER");

    private final WalletRepository walletRepository;
    private final TransactionRepository transactionRepository;
    private final RecurrenceDecisionRepository decisionRepository;
    private final CreditCardRepository creditCardRepository;
    private final LoanRepository loanRepository;
    private final UserService userService;
    private final Clock clock;

    public FutureService(
            WalletRepository walletRepository,
            TransactionRepository transactionRepository,
            RecurrenceDecisionRepository decisionRepository,
            CreditCardRepository creditCardRepository,
            LoanRepository loanRepository,
            UserService userService,
            Clock clock) {
        this.walletRepository = walletRepository;
        this.transactionRepository = transactionRepository;
        this.decisionRepository = decisionRepository;
        this.creditCardRepository = creditCardRepository;
        this.loanRepository = loanRepository;
        this.userService = userService;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public FutureResponses.Overview overview(long userId, int horizonDays) {
        Context context = load(userId);
        LocalDate end = context.today().plusDays(horizonDays);
        List<CashEvent> events = context.plan(end);
        ProjectionEngine.Projection projection = ProjectionEngine.project(context.today(), context.balance(), events, horizonDays);

        LocalDate committedUntil = context.today().plusDays(COMMITTED_WINDOW_DAYS);
        List<CashEvent> committedEvents = context.plan(committedUntil).stream()
                .filter(event -> !event.isInflow())
                .sorted(Comparator.comparing(CashEvent::date))
                .toList();
        FutureResponses.Committed committed = committed(committedUntil, committedEvents);

        long pending = context.patterns().stream().filter(pattern -> !context.decisions().containsKey(pattern.key())).count();
        return new FutureResponses.Overview(
                clock.instant(),
                context.today(),
                context.balance(),
                context.balance().subtract(committed.total()),
                committed,
                toResponse(projection, horizonDays),
                new FutureResponses.Sources(
                        context.wallets().size(),
                        context.cards().size(),
                        context.loans().size(),
                        context.confirmed().size(),
                        (int) pending
                )
        );
    }

    @Transactional(readOnly = true)
    public List<FutureResponses.Recurrence> recurrences(long userId) {
        Context context = load(userId);
        return context.patterns().stream().map(pattern -> {
            RecurrenceDecision decision = context.decisions().get(pattern.key());
            return new FutureResponses.Recurrence(
                    pattern.key(),
                    pattern.counterparty(),
                    pattern.direction().name(),
                    pattern.accountName(),
                    pattern.institution(),
                    pattern.typicalAmount(),
                    pattern.fixedAmount(),
                    pattern.typicalDay(),
                    pattern.confidence().name(),
                    pattern.suggestedCategory().name(),
                    decision == null ? "PENDING" : decision.getStatus().name(),
                    decision == null ? null : decision.getCategory(),
                    decision == null ? null : decision.getDecidedAt(),
                    pattern.occurrences().stream()
                            .map(item -> new FutureResponses.Occurrence(item.transactionId(), item.date(), item.amount()))
                            .toList()
            );
        }).toList();
    }

    @Transactional
    public void decide(long userId, FutureRequests.RecurrenceDecision request) {
        RecurrencePattern pattern = RecurrenceDetector.detect(movements(userId, walletRepository.findByUserId(userId))).stream()
                .filter(item -> item.key().equals(request.key()))
                .findFirst()
                .orElseThrow(() -> new WalletRuleException("RECURRENCE_NOT_FOUND"));

        var existing = decisionRepository.findByUserIdAndPatternKey(userId, pattern.key());
        if ("PENDING".equals(request.status())) {
            existing.ifPresent(decisionRepository::delete);
            return;
        }
        String category = request.category() == null ? pattern.suggestedCategory().name() : request.category();
        Set<String> allowed = pattern.direction() == Direction.IN ? INCOME_CATEGORIES : EXPENSE_CATEGORIES;
        if (!allowed.contains(category)) throw new WalletRuleException("RECURRENCE_CATEGORY_MISMATCH");

        RecurrenceDecision decision = existing.orElseGet(RecurrenceDecision::new);
        decision.setUser(userService.getReferenceById(userId));
        decision.setPatternKey(pattern.key());
        decision.setStatus(RecurrenceStatus.valueOf(request.status()));
        decision.setCategory(category);
        decision.setDecidedAt(clock.instant());
        decisionRepository.save(decision);
    }

    @Transactional(readOnly = true)
    public FutureResponses.Commitments commitments(long userId) {
        LocalDate today = LocalDate.now(clock.withZone(ZONE));
        List<FutureResponses.Card> cards = creditCardRepository.findByUserIdOrderById(userId).stream()
                .map(card -> toResponse(toPlan(card), card, today))
                .toList();
        List<FutureResponses.Loan> loans = loanRepository.findByUserIdOrderById(userId).stream()
                .map(loan -> {
                    LoanPlan plan = toPlan(loan);
                    List<LocalDate> dues = plan.dueDates();
                    return new FutureResponses.Loan(
                            loan.getId(),
                            loan.getProduct(),
                            loan.getInstitution(),
                            loan.getContractedAmount(),
                            loan.getOutstandingBalance(),
                            loan.getInstallmentAmount(),
                            loan.getInstallmentsTotal(),
                            loan.getInstallmentsPaid(),
                            plan.remainingInstallments(),
                            loan.getNextDueDate(),
                            dues.isEmpty() ? null : dues.get(dues.size() - 1),
                            loan.getAnnualRate(),
                            loan.getLastSyncedAt()
                    );
                })
                .toList();
        return new FutureResponses.Commitments(cards, loans);
    }

    @Transactional(readOnly = true)
    public FutureResponses.ScenarioResult simulate(long userId, FutureRequests.Scenario request) {
        ScenarioSimulator.Type type = ScenarioSimulator.Type.valueOf(request.type());
        if (type != ScenarioSimulator.Type.INCOME_LOSS && request.amount() == null) {
            throw new WalletRuleException("INVALID_AMOUNT");
        }
        Context context = load(userId);
        LocalDate end = context.today().plusDays(SCENARIO_HORIZON_DAYS);
        List<CashEvent> baseline = context.plan(end);
        if (type == ScenarioSimulator.Type.INCOME_LOSS && baseline.stream().noneMatch(CashEvent::isInflow)) {
            throw new WalletRuleException("SCENARIO_NO_CONFIRMED_INCOME");
        }

        ScenarioSimulator.Scenario scenario = new ScenarioSimulator.Scenario(
                type,
                request.amount(),
                request.installments() == null ? 1 : request.installments(),
                request.method() == null ? ScenarioSimulator.Method.CASH : ScenarioSimulator.Method.valueOf(request.method()),
                request.cardId(),
                request.dayOfMonth(),
                request.months() == null ? 1 : request.months(),
                request.incomeKey(),
                request.label()
        );
        ScenarioSimulator.Outcome outcome = ScenarioSimulator.apply(scenario, context.today(), end, baseline, context.cardPlans());
        ProjectionEngine.Projection before = ProjectionEngine.project(context.today(), context.balance(), baseline, SCENARIO_HORIZON_DAYS);
        ProjectionEngine.Projection after = ProjectionEngine.project(context.today(), context.balance(), outcome.events(), SCENARIO_HORIZON_DAYS);
        return new FutureResponses.ScenarioResult(
                summary(before),
                summary(after),
                outcome.newMonthlyCommitment(),
                outcome.installments(),
                outcome.cardName(),
                toResponse(before, SCENARIO_HORIZON_DAYS),
                toResponse(after, SCENARIO_HORIZON_DAYS)
        );
    }

    // --- loading -----------------------------------------------------------------------------

    private Context load(long userId) {
        LocalDate today = LocalDate.now(clock.withZone(ZONE));
        List<Wallet> wallets = walletRepository.findByUserId(userId);
        BigDecimal balance = wallets.stream().map(Wallet::getBalance).reduce(BigDecimal.ZERO, BigDecimal::add);
        List<RecurrencePattern> patterns = RecurrenceDetector.detect(movements(userId, wallets));
        Map<String, RecurrenceDecision> decisions = decisionRepository.findByUserId(userId).stream()
                .collect(Collectors.toMap(RecurrenceDecision::getPatternKey, Function.identity()));
        List<ConfirmedRecurrence> confirmed = patterns.stream()
                .filter(pattern -> {
                    RecurrenceDecision decision = decisions.get(pattern.key());
                    return decision != null && decision.getStatus() == RecurrenceStatus.CONFIRMED;
                })
                .map(pattern -> new ConfirmedRecurrence(pattern, Category.valueOf(decisions.get(pattern.key()).getCategory())))
                .toList();
        List<CreditCard> cards = creditCardRepository.findByUserIdOrderById(userId);
        List<Loan> loans = loanRepository.findByUserIdOrderById(userId);
        return new Context(today, wallets, balance, patterns, decisions, confirmed,
                cards, cards.stream().map(this::toPlan).toList(), loans, loans.stream().map(this::toPlan).toList());
    }

    /**
     * Settled movements from the user's point of view. Transfers between the user's own wallets
     * are skipped: they move money, they don't create income or expense.
     */
    List<Movement> movements(long userId, List<Wallet> wallets) {
        Set<Long> own = wallets.stream().map(Wallet::getId).collect(Collectors.toSet());
        Map<Long, Wallet> byId = wallets.stream().collect(Collectors.toMap(Wallet::getId, Function.identity()));
        List<Movement> movements = new ArrayList<>();
        for (Transaction transaction : transactionRepository.findAllByUserId(userId)) {
            if (transaction.getStatus() != Status.SUCCESS) continue;
            Long from = transaction.getFromWallet().getId();
            Long to = transaction.getToWallet().getId();
            long typeId = transaction.getType().getId();
            LocalDate date = LocalDate.ofInstant(transaction.getCreatedAt(), ZONE);
            Direction direction;
            Wallet account;
            String counterparty;
            if (Objects.equals(from, to) && (typeId == TYPE_DEPOSIT || typeId == TYPE_WITHDRAWAL)) {
                direction = typeId == TYPE_DEPOSIT ? Direction.IN : Direction.OUT;
                account = byId.get(from);
                counterparty = transaction.getSource() == TransactionSource.OPEN_FINANCE
                        ? transaction.getDescription()
                        : (direction == Direction.IN ? "Depósito" : "Retirada");
            } else if (own.contains(from) && own.contains(to)) {
                continue;
            } else if (own.contains(from)) {
                direction = Direction.OUT;
                account = byId.get(from);
                counterparty = "Transferência enviada";
            } else if (own.contains(to)) {
                direction = Direction.IN;
                account = byId.get(to);
                counterparty = "Transferência recebida";
            } else {
                continue;
            }
            if (account == null) continue;
            movements.add(new Movement(transaction.getId(), date, transaction.getAmount(), direction, counterparty,
                    account.getName(), account.getInstitutionName()));
        }
        return movements;
    }

    private CardPlan toPlan(CreditCard card) {
        return new CardPlan(
                card.getId(),
                card.getName() + (card.getLastDigits() == null ? "" : " · final " + card.getLastDigits()),
                card.getInstitution(),
                card.getClosingDay(),
                card.getDueDay(),
                card.getCharges().stream()
                        .map(charge -> new CardPlan.Charge(
                                charge.getId(),
                                charge.getDescription(),
                                charge.getPurchaseDate(),
                                charge.getTotalAmount(),
                                charge.getInstallmentAmount(),
                                charge.getInstallments(),
                                YearMonth.from(charge.getFirstInvoice())))
                        .toList()
        );
    }

    private LoanPlan toPlan(Loan loan) {
        return new LoanPlan(
                loan.getId(),
                loan.getProduct(),
                loan.getInstitution(),
                loan.getOutstandingBalance(),
                loan.getInstallmentAmount(),
                loan.getInstallmentsTotal(),
                loan.getInstallmentsPaid(),
                loan.getNextDueDate()
        );
    }

    // --- mapping -----------------------------------------------------------------------------

    private FutureResponses.Committed committed(LocalDate until, List<CashEvent> outflows) {
        Map<String, List<CashEvent>> groups = outflows.stream()
                .collect(Collectors.groupingBy(CashEvent::category, LinkedHashMap::new, Collectors.toList()));
        List<FutureResponses.CommittedGroup> mapped = groups.entrySet().stream()
                .map(entry -> new FutureResponses.CommittedGroup(
                        entry.getKey(),
                        sumOut(entry.getValue()),
                        entry.getValue().stream().map(this::toResponse).toList()))
                .sorted(Comparator.comparing(FutureResponses.CommittedGroup::total).reversed())
                .toList();
        return new FutureResponses.Committed(
                until,
                sumOut(outflows),
                sumOut(outflows.stream().filter(event -> event.certainty() == CashEvent.Certainty.CONFIRMED).toList()),
                sumOut(outflows.stream().filter(event -> event.certainty() == CashEvent.Certainty.ESTIMATED).toList()),
                mapped
        );
    }

    private BigDecimal sumOut(List<CashEvent> events) {
        return events.stream().map(event -> event.amount().negate()).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private FutureResponses.Card toResponse(CardPlan plan, CreditCard card, LocalDate today) {
        YearMonth first = YearMonth.from(today);
        if (plan.dueDate(first).isBefore(today)) first = first.plusMonths(1);
        YearMonth open = plan.invoiceFor(today);
        List<FutureResponses.Invoice> invoices = new ArrayList<>();
        BigDecimal committedLimit = BigDecimal.ZERO;
        YearMonth lastBooked = plan.charges().stream()
                .map(charge -> charge.firstInvoice().plusMonths(charge.installments() - 1L))
                .max(Comparator.naturalOrder())
                .orElse(first);
        for (YearMonth invoice = first; !invoice.isAfter(lastBooked); invoice = invoice.plusMonths(1)) {
            BigDecimal total = plan.invoiceTotal(invoice);
            committedLimit = committedLimit.add(total);
            if (invoices.size() >= 6) continue;
            String status = invoice.isBefore(open) ? "CLOSED" : invoice.equals(open) ? "OPEN" : "FUTURE";
            invoices.add(new FutureResponses.Invoice(
                    invoice.toString(),
                    plan.closingDate(plan.dueDay() > plan.closingDay() ? invoice : invoice.minusMonths(1)),
                    plan.dueDate(invoice),
                    status,
                    total,
                    plan.invoiceLines(invoice).stream()
                            .map(line -> new FutureResponses.InvoiceLine(
                                    line.charge().description(),
                                    line.charge().purchaseDate(),
                                    line.installment(),
                                    line.charge().installments(),
                                    line.charge().installmentAmount()))
                            .toList()
            ));
        }
        return new FutureResponses.Card(
                card.getId(),
                card.getName(),
                card.getInstitution(),
                card.getLastDigits(),
                card.getCreditLimit(),
                committedLimit,
                card.getClosingDay(),
                card.getDueDay(),
                invoices
        );
    }

    private FutureResponses.Projection toResponse(ProjectionEngine.Projection projection, int horizonDays) {
        return new FutureResponses.Projection(
                horizonDays,
                projection.openingBalance(),
                projection.minBalance(),
                projection.minDate(),
                projection.endBalance(),
                projection.negativeDays(),
                projection.days().stream()
                        .map(day -> new FutureResponses.DayPoint(day.date(), day.balance(), day.inflow(), day.outflow()))
                        .toList(),
                projection.riskWindows().stream()
                        .map(window -> new FutureResponses.RiskWindow(
                                window.start(),
                                window.end(),
                                window.lowestBalance(),
                                window.lowestDate(),
                                window.untilHorizon(),
                                window.causes().stream().map(this::toResponse).toList(),
                                window.relief() == null ? null : toResponse(window.relief())))
                        .toList(),
                projection.events().stream().map(this::toResponse).toList()
        );
    }

    private FutureResponses.Event toResponse(CashEvent event) {
        return new FutureResponses.Event(
                event.date(),
                event.amount(),
                event.label(),
                event.kind().name(),
                event.category(),
                event.certainty().name(),
                event.origin(),
                event.reference()
        );
    }

    private FutureResponses.ScenarioSummary summary(ProjectionEngine.Projection projection) {
        LocalDate limit30 = projection.start().plusDays(COMMITTED_WINDOW_DAYS);
        BigDecimal min30 = projection.days().stream()
                .filter(day -> !day.date().isAfter(limit30))
                .map(ProjectionEngine.DayPoint::balance)
                .min(BigDecimal::compareTo)
                .orElse(projection.openingBalance());
        LocalDate firstNegative = projection.days().stream()
                .filter(day -> day.balance().signum() < 0)
                .map(ProjectionEngine.DayPoint::date)
                .findFirst()
                .orElse(null);
        return new FutureResponses.ScenarioSummary(
                min30,
                projection.minBalance(),
                projection.minDate(),
                firstNegative,
                projection.negativeDays(),
                projection.endBalance()
        );
    }

    private record Context(
            LocalDate today,
            List<Wallet> wallets,
            BigDecimal balance,
            List<RecurrencePattern> patterns,
            Map<String, RecurrenceDecision> decisions,
            List<ConfirmedRecurrence> confirmed,
            List<CreditCard> cards,
            List<CardPlan> cardPlans,
            List<Loan> loans,
            List<LoanPlan> loanPlans
    ) {

        List<CashEvent> plan(LocalDate end) {
            return CashEventPlanner.plan(today, end, confirmed, cardPlans, loanPlans);
        }
    }
}
