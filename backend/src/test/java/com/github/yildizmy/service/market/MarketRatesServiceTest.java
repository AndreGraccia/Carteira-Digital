package com.github.yildizmy.service.market;

import com.github.yildizmy.domain.entity.MarketIndicator;
import com.github.yildizmy.domain.entity.MarketRate;
import com.github.yildizmy.domain.entity.OpenFinanceConnection;
import com.github.yildizmy.repository.MarketIndicatorRepository;
import com.github.yildizmy.repository.MarketRateRepository;
import com.github.yildizmy.repository.OpenFinanceConnectionRepository;
import com.github.yildizmy.service.future.OverdraftCost;
import com.github.yildizmy.service.job.JobLock;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.transaction.support.TransactionOperations;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class MarketRatesServiceTest {

    private final MarketRateRepository rates = mock(MarketRateRepository.class);
    private final MarketIndicatorRepository indicators = mock(MarketIndicatorRepository.class);
    private final OpenFinanceConnectionRepository connections = mock(OpenFinanceConnectionRepository.class);
    private final Clock clock = Clock.fixed(Instant.parse("2026-09-25T09:00:00Z"), ZoneOffset.UTC);

    private MarketRatesService service(MarketRatesService.Fetcher fetcher) {
        return new MarketRatesService(rates, indicators, connections, TransactionOperations.withoutTransaction(),
                JobLock.inProcess(), clock, false, fetcher);
    }

    private static String resource(String name) throws IOException {
        try (var in = MarketRatesServiceTest.class.getResourceAsStream("/bcb/" + name)) {
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    @Test
    @SuppressWarnings("unchecked")
    void storesTheLatestWindowAndEachIndicatorAndOneFailingSeriesDoesNotStopTheOthers() throws IOException {
        String olinda = resource("taxas-diarias-por-periodo.json");
        String cdi = resource("sgs-4389.json");
        MarketRatesService service = service(url -> {
            if (url.equals(MarketRatesService.OLINDA)) return olinda;
            if (url.contains("sgs.4389/")) return cdi;
            throw new IOException("HTTP 502");
        });

        MarketRatesService.Result result = service.refresh();

        assertEquals(new MarketRatesService.Result(21, 1), result);
        verify(rates).deletePeriod(LocalDate.of(2026, 9, 3));
        ArgumentCaptor<List<MarketRate>> saved = ArgumentCaptor.forClass(List.class);
        verify(rates).saveAll(saved.capture());
        assertEquals(21, saved.getValue().size());
        ArgumentCaptor<MarketIndicator> indicator = ArgumentCaptor.forClass(MarketIndicator.class);
        verify(indicators).save(indicator.capture());
        assertEquals("CDI", indicator.getValue().getCode());
        assertEquals(clock.instant(), indicator.getValue().getFetchedAt());
    }

    @Test
    void bancoCentralDownStoresNothingAndDoesNotThrow() {
        MarketRatesService service = service(url -> {
            throw new IOException("connect timed out");
        });

        assertEquals(new MarketRatesService.Result(0, 0), service.refresh());
        verify(rates, never()).deletePeriod(any());
    }

    @Test
    void theUsersBankRateComesFirstThenTheMarketMedian() {
        when(rates.findLatest("OVERDRAFT")).thenReturn(List.of(
                rate("59285411", "BANCO PAN", "0.63"), rate("92894922", "BANCO ORIGINAL", "2.94"),
                rate("02038232", "BANCO SICOOB S.A.", "4.42")));
        OpenFinanceConnection connection = new OpenFinanceConnection();
        connection.setInstitutionId("original-92894922000183");
        when(connections.findFirstByUserIdOrderByIdDesc(1L)).thenReturn(Optional.of(connection));
        when(connections.findFirstByUserIdOrderByIdDesc(2L)).thenReturn(Optional.empty());
        MarketRatesService service = service(url -> "");

        OverdraftCost.Rate bank = service.overdraftRate(1L);
        OverdraftCost.Rate market = service.overdraftRate(2L);

        assertEquals(OverdraftCost.Source.BANK, bank.source());
        assertEquals("BANCO ORIGINAL", bank.institution());
        assertEquals(new BigDecimal("2.94"), bank.monthlyPercent());
        assertEquals(OverdraftCost.Source.MARKET, market.source());
        assertEquals(new BigDecimal("2.94"), market.monthlyPercent());
        MarketRatesService.Quote quote = service.quote(BcbData.Modality.OVERDRAFT, 2L);
        assertEquals(3, quote.institutions());
        assertEquals(new BigDecimal("0.63"), quote.lowest());
        assertEquals(new BigDecimal("4.42"), quote.highest());
    }

    @Test
    void withoutStoredRatesThereIsNoQuoteAndTheEngineKeepsTheCap() {
        when(rates.findLatest(any())).thenReturn(List.of());

        assertNull(service(url -> "").overdraftRate(1L));
        assertTrue(service(url -> "").quotes(1L).isEmpty());
    }

    private static MarketRate rate(String cnpj8, String name, String monthly) {
        MarketRate rate = new MarketRate();
        rate.setModality("OVERDRAFT");
        rate.setPeriodStart(LocalDate.of(2026, 9, 3));
        rate.setPeriodEnd(LocalDate.of(2026, 9, 10));
        rate.setCnpj8(cnpj8);
        rate.setInstitution(name);
        rate.setMonthlyRate(new BigDecimal(monthly));
        rate.setAnnualRate(BigDecimal.TEN);
        return rate;
    }
}
