package com.github.yildizmy.service.market;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import com.github.yildizmy.domain.entity.MarketIndicator;
import com.github.yildizmy.domain.entity.MarketRate;
import com.github.yildizmy.repository.MarketIndicatorRepository;
import com.github.yildizmy.repository.MarketRateRepository;
import com.github.yildizmy.repository.OpenFinanceConnectionRepository;
import com.github.yildizmy.service.future.OverdraftCost;
import com.github.yildizmy.service.job.JobLock;
import com.github.yildizmy.service.market.BcbData.Modality;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionOperations;

import java.io.IOException;
import java.math.BigDecimal;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Clock;
import java.time.Duration;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Banco Central public data kept once a day in the database (ADR 0010): each institution's rates for individuals
 * (Olinda taxaJuros v2) and CDI, 12-month IPCA and Selic (SGS). The BCB being down never breaks a screen: the last
 * stored values stay, and without any the overdraft falls back to the legal cap.
 */
@Slf4j
@Service
public class MarketRatesService {

    /** Latest windows first, individuals only; one window has ~500 rows, so 6000 always holds the newest whole. */
    static final String OLINDA = "https://olinda.bcb.gov.br/olinda/servico/taxaJuros/versao/v2/odata/"
            + "TaxasJurosDiariaPorInicioPeriodo?%24top=6000&%24orderby=InicioPeriodo%20desc&%24format=json"
            + "&%24filter=Segmento%20eq%20%27PESSOA%20F%C3%8DSICA%27";
    static final Map<String, Integer> SGS = Map.of("CDI", 4389, "IPCA_12M", 13522, "SELIC", 432);
    private static final String SGS_URL = "https://api.bcb.gov.br/dados/serie/bcdata.sgs.%d/dados/ultimos/1?formato=json";

    /** Fetches a URL and returns the body, or throws. Replaced in tests. */
    interface Fetcher {
        String get(String url) throws IOException, InterruptedException;
    }

    private final MarketRateRepository rates;
    private final MarketIndicatorRepository indicators;
    private final OpenFinanceConnectionRepository connections;
    private final TransactionOperations transactions;
    private final JobLock jobLock;
    private final Fetcher fetcher;
    private final Clock clock;
    private final ObjectMapper mapper = new ObjectMapper();
    private final boolean refreshOnStartup;

    @Autowired
    public MarketRatesService(MarketRateRepository rates, MarketIndicatorRepository indicators,
                              OpenFinanceConnectionRepository connections, TransactionOperations transactions,
                              JobLock jobLock, Clock clock,
                              @Value("${app.market.refresh-on-startup:true}") boolean refreshOnStartup) {
        this(rates, indicators, connections, transactions, jobLock, clock, refreshOnStartup, httpFetcher());
    }

    MarketRatesService(MarketRateRepository rates, MarketIndicatorRepository indicators,
                       OpenFinanceConnectionRepository connections, TransactionOperations transactions,
                       JobLock jobLock, Clock clock, boolean refreshOnStartup, Fetcher fetcher) {
        this.rates = rates;
        this.indicators = indicators;
        this.connections = connections;
        this.transactions = transactions;
        this.jobLock = jobLock;
        this.clock = clock;
        this.refreshOnStartup = refreshOnStartup;
        this.fetcher = fetcher;
    }

    private static Fetcher httpFetcher() {
        HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();
        return url -> {
            HttpResponse<String> response = client.send(
                    HttpRequest.newBuilder(URI.create(url)).timeout(Duration.ofSeconds(30))
                            .header("Accept", "application/json").GET().build(),
                    HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) throw new IOException("HTTP " + response.statusCode());
            return response.body();
        };
    }

    /** A fresh database has no rates until the first 6h run; fetch once at startup so the screens have them. */
    @EventListener(ApplicationReadyEvent.class)
    public void onStartup() {
        if (refreshOnStartup && rates.count() == 0) scheduled();
    }

    /** Olinda publishes each business day; one instance fetches, the others skip (advisory lock). */
    @Scheduled(cron = "${app.market.refresh-cron:0 0 6 * * *}", zone = "America/Sao_Paulo")
    public void scheduled() {
        jobLock.runExclusive("market-rates-refresh", this::refresh);
    }

    /** Fetches everything outside any transaction, then stores each part on its own; a failed part is skipped. */
    public Result refresh() {
        int stored = 0;
        try {
            // One row per institution and modality (the table's key), first one wins.
            List<BcbData.Rate> latest = List.copyOf(BcbData.latestRates(mapper.readTree(fetcher.get(OLINDA))).stream()
                    .collect(java.util.stream.Collectors.toMap(rate -> rate.modality() + rate.cnpj8(), rate -> rate,
                            (first, second) -> first, java.util.LinkedHashMap::new))
                    .values());
            if (!latest.isEmpty()) {
                LocalDate week = latest.get(0).periodStart();
                transactions.executeWithoutResult(status -> {
                    rates.deletePeriod(week);
                    rates.saveAll(latest.stream().map(MarketRatesService::entity).toList());
                });
                stored = latest.size();
            }
        } catch (IOException | RuntimeException exception) {
            log.warn("Banco Central rates not refreshed: {}", exception.getMessage());
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            return new Result(0, 0);
        }
        int indicatorsStored = 0;
        for (Map.Entry<String, Integer> series : SGS.entrySet()) {
            try {
                Optional<BcbData.Indicator> last = BcbData.lastObservation(series.getKey(),
                        mapper.readTree(fetcher.get(SGS_URL.formatted(series.getValue()))));
                if (last.isEmpty()) continue;
                MarketIndicator row = new MarketIndicator();
                row.setCode(last.get().code());
                row.setReferenceDate(last.get().referenceDate());
                row.setValue(last.get().value());
                row.setFetchedAt(clock.instant());
                indicators.save(row);
                indicatorsStored++;
            } catch (IOException | RuntimeException exception) {
                log.warn("SGS {} not refreshed: {}", series.getValue(), exception.getMessage());
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
                break;
            }
        }
        log.info("Banco Central data: {} rates, {} indicators", stored, indicatorsStored);
        return new Result(stored, indicatorsStored);
    }

    public record Result(int rates, int indicators) {
    }

    /** Overdraft rate for this user: their bank's, else the market median, else null (the engine uses the cap). */
    public OverdraftCost.Rate overdraftRate(long userId) {
        Quote quote = quote(Modality.OVERDRAFT, userId);
        if (quote == null) return null;
        return quote.bankRate() != null
                ? new OverdraftCost.Rate(quote.bankRate(), OverdraftCost.Source.BANK, quote.bankName(), quote.periodStart())
                : new OverdraftCost.Rate(quote.median(), OverdraftCost.Source.MARKET, null, quote.periodStart());
    }

    /** Monthly rates in percent for one modality: market median, spread and, when found, the user's bank. */
    public record Quote(Modality modality, LocalDate periodStart, LocalDate periodEnd, int institutions,
                        BigDecimal median, BigDecimal lowest, BigDecimal highest,
                        String bankName, BigDecimal bankRate) {
    }

    public Quote quote(Modality modality, long userId) {
        List<MarketRate> latest = rates.findLatest(modality.name());
        if (latest.isEmpty()) return null;
        List<BigDecimal> monthly = latest.stream().map(MarketRate::getMonthlyRate).sorted().toList();
        String cnpj8 = userCnpj8(userId);
        MarketRate bank = cnpj8 == null ? null
                : latest.stream().filter(rate -> rate.getCnpj8().equals(cnpj8)).findFirst().orElse(null);
        return new Quote(modality, latest.get(0).getPeriodStart(), latest.get(0).getPeriodEnd(), latest.size(),
                BcbData.median(monthly), monthly.get(0), monthly.get(monthly.size() - 1),
                bank == null ? null : bank.getInstitution(), bank == null ? null : bank.getMonthlyRate());
    }

    public List<MarketIndicator> indicators() {
        return indicators.findAll();
    }

    /**
     * The first 8 digits of the connected institution's CNPJ, the key Banco Central publishes rates under.
     * Institution ids without a CNPJ (a participant sandbox) find no bank and fall back to the median.
     */
    private String userCnpj8(long userId) {
        return connections.findFirstByUserIdOrderByIdDesc(userId)
                .map(connection -> connection.getInstitutionId() == null ? ""
                        : connection.getInstitutionId().replaceAll("\\D", ""))
                .filter(digits -> digits.length() >= 8)
                .map(digits -> digits.substring(0, 8))
                .orElse(null);
    }

    private static MarketRate entity(BcbData.Rate rate) {
        MarketRate row = new MarketRate();
        row.setModality(rate.modality().name());
        row.setPeriodStart(rate.periodStart());
        row.setPeriodEnd(rate.periodEnd());
        row.setCnpj8(rate.cnpj8());
        row.setInstitution(rate.institution().length() > 120 ? rate.institution().substring(0, 120) : rate.institution());
        row.setMonthlyRate(rate.monthlyRate());
        row.setAnnualRate(rate.annualRate());
        return row;
    }

    /** Kept for the overview: a quote per modality, null ones dropped. */
    public List<Quote> quotes(long userId) {
        List<Quote> quotes = new ArrayList<>();
        for (Modality modality : Modality.values()) {
            Quote quote = quote(modality, userId);
            if (quote != null) quotes.add(quote);
        }
        return quotes;
    }
}
