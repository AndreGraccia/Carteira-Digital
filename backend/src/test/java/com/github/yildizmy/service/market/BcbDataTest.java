package com.github.yildizmy.service.market;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/** Against real Banco Central responses saved in src/test/resources/bcb. */
class BcbDataTest {

    static JsonNode fixture(String name) throws IOException {
        try (InputStream in = BcbDataTest.class.getResourceAsStream("/bcb/" + name)) {
            return new ObjectMapper().readTree(in);
        }
    }

    @Test
    void keepsOnlyTheLatestWindowOfIndividualsInTheThreeModalitiesTheAppUses() throws IOException {
        List<BcbData.Rate> rates = BcbData.latestRates(fixture("taxas-diarias-por-periodo.json"));

        assertEquals(21, rates.size());
        assertTrue(rates.stream().allMatch(rate -> rate.periodStart().equals(LocalDate.of(2026, 9, 3))));
        assertEquals(7, rates.stream().filter(rate -> rate.modality() == BcbData.Modality.OVERDRAFT).count());
        BcbData.Rate pan = rates.get(0);
        assertEquals("59285411", pan.cnpj8());
        assertEquals("BANCO PAN", pan.institution());
        assertEquals(0, new BigDecimal("0.63").compareTo(pan.monthlyRate()));
    }

    @Test
    void medianOfTheOverdraftRatesIsTheMiddleBank() throws IOException {
        List<BigDecimal> overdraft = BcbData.latestRates(fixture("taxas-diarias-por-periodo.json")).stream()
                .filter(rate -> rate.modality() == BcbData.Modality.OVERDRAFT)
                .map(BcbData.Rate::monthlyRate).toList();

        assertEquals(0, new BigDecimal("2.94").compareTo(BcbData.median(overdraft)));
        assertEquals(new BigDecimal("2.0000"), BcbData.median(List.of(BigDecimal.ONE, new BigDecimal("3"))));
        assertNull(BcbData.median(List.of()));
    }

    @Test
    void readsTheLastSgsObservationWithItsBrazilianDate() throws IOException {
        BcbData.Indicator cdi = BcbData.lastObservation("CDI", fixture("sgs-4389.json")).orElseThrow();

        assertEquals(LocalDate.of(2026, 9, 23), cdi.referenceDate());
        assertEquals(new BigDecimal("13.65"), cdi.value());
        assertTrue(BcbData.lastObservation("CDI", new ObjectMapper().createArrayNode()).isEmpty());
    }
}
