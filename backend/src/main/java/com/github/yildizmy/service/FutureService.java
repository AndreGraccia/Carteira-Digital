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
import com.github.yildizmy.service.future.DailySpending;
import com.github.yildizmy.service.future.LoanPlan;
import com.github.yildizmy.service.future.ProjectionEngine;
import com.github.yildizmy.service.future.QuestionParser;
import com.github.yildizmy.service.future.RecurrenceDetector;
import com.github.yildizmy.service.future.RecurrenceDetector.Category;
import com.github.yildizmy.service.future.RecurrenceDetector.Direction;
import com.github.yildizmy.service.future.RecurrenceDetector.Movement;
import com.github.yildizmy.service.future.RecurrenceDetector.RecurrencePattern;
import com.github.yildizmy.service.future.ScenarioSimulator;
import com.github.yildizmy.service.future.SpendingLimit;
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
import java.util.Optional;
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
    /** Decision row that stores the everyday-spending answer; pattern keys always start with "in:" or "out:". */
    static final String DAILY_SPENDING_KEY = "daily-spending";

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
        SpendingLimit.Limit limit = SpendingLimit.compute(context.today(), context.balance(),
                context.planFixed(context.today().plusDays(SpendingLimit.MAX_DAYS)));
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
                ),
                dailySpending(context),
                new FutureResponses.SpendingLimit(
                        limit.daily(),
                        limit.until(),
                        limit.untilIncome(),
                        limit.days(),
                        limit.bindingDate(),
                        context.dailyAmount(),
                        context.dailyAmount() == null ? null : limit.daily().subtract(context.dailyAmount()),
                        limit.shortfall())
        );
    }

    /**
     * Answer to "is this your everyday spending?". PENDING removes the answer; CONFIRMED may carry the
     * user's own daily value, otherwise the estimate from the history is used.
     */
    @Transactional
    public void decideDailySpending(long userId, FutureRequests.DailySpendingDecision request) {
        var existing = decisionRepository.findByUserIdAndPatternKey(userId, DAILY_SPENDING_KEY);
        if ("PENDING".equals(request.status())) {
            existing.ifPresent(decisionRepository::delete);
            return;
        }
        Context context = load(userId);
        if (context.dailyEstimate().isEmpty() && request.dailyAmount() == null) {
            throw new WalletRuleException("DAILY_SPENDING_NO_HISTORY");
        }
        RecurrenceDecision decision = existing.orElseGet(RecurrenceDecision::new);
        decision.setUser(userService.getReferenceById(userId));
        decision.setPatternKey(DAILY_SPENDING_KEY);
        decision.setStatus(RecurrenceStatus.valueOf(request.status()));
        decision.setCategory(CashEventPlanner.DAILY_SPENDING);
        decision.setAmount("CONFIRMED".equals(request.status()) ? request.dailyAmount() : null);
        decision.setDecidedAt(clock.instant());
        decisionRepository.save(decision);
    }

    private FutureResponses.DailySpending dailySpending(Context context) {
        RecurrenceDecision decision = context.decisions().get(DAILY_SPENDING_KEY);
        Optional<DailySpending.Estimate> estimate = context.dailyEstimate();
        if (estimate.isEmpty() && decision == null) return null;
        return new FutureResponses.DailySpending(
                decision == null ? "PENDING" : decision.getStatus().name(),
                estimate.map(DailySpending.Estimate::daily).orElse(null),
                context.dailyAmount(),
                decision == null ? null : decision.getAmount(),
                estimate.map(DailySpending.Estimate::total).orElse(null),
                estimate.map(DailySpending.Estimate::days).orElse(0),
                estimate.map(DailySpending.Estimate::movements).orElse(0),
                estimate.map(item -> item.top().stream()
                        .map(top -> new FutureResponses.SpendingItem(top.counterparty(), top.total(), top.count()))
                        .toList()).orElse(List.of())
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

    /** How far "Pergunte ao Wall.ia" looks for risks and the next income. */
    static final int ASK_HORIZON_DAYS = 60;

    /**
     * "Pergunte ao Wall.ia": picks the calculation that answers the question and returns its numbers.
     * Nothing is written and nothing is invented: an unknown question returns UNKNOWN.
     */
    @Transactional(readOnly = true)
    public FutureResponses.Answer ask(long userId, String question) {
        QuestionParser.Parsed parsed = QuestionParser.parse(question);
        FutureResponses.Overview overview = overview(userId, ASK_HORIZON_DAYS);
        String intent = parsed.intent().name();
        String method = parsed.card() ? "CARD" : "CASH";
        return switch (parsed.intent()) {
            case PURCHASE -> {
                if (parsed.amount() == null) {
                    yield answer(intent, parsed, method, "NEED_AMOUNT", overview, null, null, null, null, null, null, null);
                }
                FutureResponses.ScenarioResult result = simulate(userId, new FutureRequests.Scenario(
                        "PURCHASE", parsed.amount(), parsed.installments(), method, null, null, null, null, "Pergunta"));
                yield answer(intent, parsed, method, verdict(result, overview.spendingLimit()), overview,
                        overview.spendingLimit(), null, null, null, result, result.newMonthlyCommitment(), result.cardName());
            }
            case DAILY_LIMIT -> answer(intent, parsed, method, null, overview, overview.spendingLimit(),
                    null, null, null, null, null, null);
            case WHY_NEGATIVE -> answer(intent, parsed, method, null, overview, overview.spendingLimit(),
                    overview.projection().riskWindows().stream().findFirst().orElse(null), null, null, null, null, null);
            case COMMITTED -> answer(intent, parsed, method, null, overview, null, null, overview.committed(),
                    null, null, null, null);
            case NEXT_INCOME -> answer(intent, parsed, method, null, overview, null, null, null,
                    overview.projection().events().stream().filter(event -> event.amount().signum() > 0)
                            .findFirst().orElse(null), null, null, null);
            case UNKNOWN -> answer(intent, parsed, method, null, overview, null, null, null, null, null, null, null);
        };
    }

    /**
     * RISK when the purchase adds negative days; TIGHT when it adds none but leaves the lowest point below
     * a week of everyday spending (or keeps days that were already negative); SAFE otherwise.
     */
    static String verdict(FutureResponses.ScenarioResult result, FutureResponses.SpendingLimit limit) {
        FutureResponses.ScenarioSummary before = result.baseline();
        FutureResponses.ScenarioSummary after = result.scenario();
        if (after.negativeDays() > before.negativeDays()) return "RISK";
        BigDecimal weekBuffer = limit == null || limit.pace() == null
                ? BigDecimal.ZERO
                : limit.pace().multiply(BigDecimal.valueOf(7));
        if (after.negativeDays() > 0 || after.minBalance90().compareTo(weekBuffer) < 0) return "TIGHT";
        return "SAFE";
    }

    private FutureResponses.Answer answer(String intent, QuestionParser.Parsed parsed, String method, String verdict,
                                          FutureResponses.Overview overview, FutureResponses.SpendingLimit limit,
                                          FutureResponses.RiskWindow risk, FutureResponses.Committed committed,
                                          FutureResponses.Event nextIncome, FutureResponses.ScenarioResult scenario,
                                          BigDecimal newMonthlyCommitment, String cardName) {
        return new FutureResponses.Answer(
                intent,
                parsed.amount(),
                parsed.installments(),
                method,
                verdict,
                overview.balance(),
                limit,
                risk,
                committed,
                nextIncome,
                scenario == null ? null : scenario.baseline(),
                scenario == null ? null : scenario.scenario(),
                newMonthlyCommitment,
                cardName);
    }

    // --- loading -----------------------------------------------------------------------------

    private Context load(long userId) {
        LocalDate today = LocalDate.now(clock.withZone(ZONE));
        List<Wallet> wallets = walletRepository.findByUserId(userId);
        BigDecimal balance = wallets.stream().map(Wallet::getBalance).reduce(BigDecimal.ZERO, BigDecimal::add);
        List<Movement> movements = movements(userId, wallets);
        List<RecurrencePattern> patterns = RecurrenceDetector.detect(movements);
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

        // Bills already projected (confirmed) or still being asked about (pending) are not everyday spending.
        Set<String> fixedKeys = patterns.stream()
                .filter(pattern -> {
                    RecurrenceDecision decision = decisions.get(pattern.key());
                    return decision == null || decision.getStatus() == RecurrenceStatus.CONFIRMED;
                })
                .map(RecurrencePattern::key)
                .collect(Collectors.toSet());
        Optional<DailySpending.Estimate> dailyEstimate = DailySpending.estimate(today, movements, fixedKeys);
        RecurrenceDecision dailyDecision = decisions.get(DAILY_SPENDING_KEY);
        BigDecimal dailyAmount = null;
        if (dailyDecision != null && dailyDecision.getStatus() == RecurrenceStatus.CONFIRMED) {
            dailyAmount = dailyDecision.getAmount() != null
                    ? dailyDecision.getAmount()
                    : dailyEstimate.map(DailySpending.Estimate::daily).orElse(null);
        }
        return new Context(today, wallets, balance, patterns, decisions, confirmed,
                cards, cards.stream().map(this::toPlan).toList(), loans, loans.stream().map(this::toPlan).toList(),
                dailyEstimate, dailyAmount);
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
                        toEvents(entry.getValue())))
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
                                toEvents(window.causes()),
                                window.relief() == null ? null : toResponse(window.relief()),
                                window.overdraftCostCap()))
                        .toList(),
                toEvents(projection.events())
        );
    }

    /**
     * Maps events, folding the one-per-day everyday-spending events into a single line so lists stay
     * readable. The folded line keeps the total, and its reference says how many days it covers.
     */
    private List<FutureResponses.Event> toEvents(List<CashEvent> events) {
        List<CashEvent> daily = events.stream().filter(event -> event.kind() == CashEvent.Kind.DAILY_SPENDING).toList();
        List<FutureResponses.Event> mapped = new ArrayList<>(events.stream()
                .filter(event -> event.kind() != CashEvent.Kind.DAILY_SPENDING)
                .map(this::toResponse)
                .toList());
        if (!daily.isEmpty()) {
            CashEvent last = daily.get(daily.size() - 1);
            mapped.add(new FutureResponses.Event(
                    last.date(),
                    daily.stream().map(CashEvent::amount).reduce(BigDecimal.ZERO, BigDecimal::add),
                    last.label(),
                    last.kind().name(),
                    last.category(),
                    last.certainty().name(),
                    last.origin(),
                    DAILY_SPENDING_KEY + ":" + daily.size()));
            mapped.sort(Comparator.comparing(FutureResponses.Event::date));
        }
        return mapped;
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
            List<LoanPlan> loanPlans,
            Optional<DailySpending.Estimate> dailyEstimate,
            BigDecimal dailyAmount
    ) {

        /** Everything the user confirmed, everyday spending included. */
        List<CashEvent> plan(LocalDate end) {
            List<CashEvent> events = new ArrayList<>(planFixed(end));
            events.addAll(CashEventPlanner.dailySpending(dailyAmount, today, end,
                    dailyEstimate.map(estimate -> "Média dos últimos " + estimate.days() + " dias").orElse("Valor informado")));
            return events;
        }

        /** Bills, invoices and instalments only: the base for the daily spending limit. */
        List<CashEvent> planFixed(LocalDate end) {
            return CashEventPlanner.plan(today, end, confirmed, cardPlans, loanPlans);
        }
    }
}
