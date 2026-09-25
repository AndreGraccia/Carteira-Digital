package com.github.yildizmy.service.market;

import com.fasterxml.jackson.databind.JsonNode;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Reads Banco Central public data. Pure functions, tested against real responses saved in
 * {@code src/test/resources/bcb}.
 */
public final class BcbData {

    /** Olinda modality names (segment PESSOA FÍSICA) the app uses. */
    static final Map<String, Modality> MODALITIES = Map.of(
            "Cheque especial - Prefixado", Modality.OVERDRAFT,
            "Cartão de crédito - rotativo total - Prefixado", Modality.CARD_REVOLVING,
            "Crédito pessoal não consignado - Prefixado", Modality.PERSONAL_LOAN);

    private static final DateTimeFormatter SGS_DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private BcbData() {
    }

    public enum Modality { OVERDRAFT, CARD_REVOLVING, PERSONAL_LOAN }

    /** Monthly and yearly rate, in percent, of one institution for one week. */
    public record Rate(Modality modality, LocalDate periodStart, LocalDate periodEnd, String cnpj8, String institution,
                       BigDecimal monthlyRate, BigDecimal annualRate) {
    }

    public record Indicator(String code, LocalDate referenceDate, BigDecimal value) {
    }

    /**
     * Rates of the most recent week in the response, individuals only, for the modalities the app uses.
     * Older weeks and company (PESSOA JURÍDICA) rows are dropped.
     */
    public static List<Rate> latestRates(JsonNode olinda) {
        List<Rate> rates = new ArrayList<>();
        for (JsonNode row : olinda.path("value")) {
            Modality modality = MODALITIES.get(row.path("Modalidade").asText());
            if (modality == null || !"PESSOA FÍSICA".equals(row.path("Segmento").asText())) continue;
            rates.add(new Rate(modality,
                    LocalDate.parse(row.path("InicioPeriodo").asText()),
                    LocalDate.parse(row.path("FimPeriodo").asText()),
                    row.path("cnpj8").asText(),
                    row.path("InstituicaoFinanceira").asText(),
                    new BigDecimal(row.path("TaxaJurosAoMes").asText()),
                    new BigDecimal(row.path("TaxaJurosAoAno").asText())));
        }
        Optional<LocalDate> latest = rates.stream().map(Rate::periodStart).max(Comparator.naturalOrder());
        return latest.map(week -> rates.stream().filter(rate -> rate.periodStart().equals(week)).toList())
                .orElse(List.of());
    }

    /** The last observation of an SGS series ({@code [{"data":"23/09/2026","valor":"13.65"}]}). */
    public static Optional<Indicator> lastObservation(String code, JsonNode sgs) {
        if (!sgs.isArray() || sgs.isEmpty()) return Optional.empty();
        JsonNode last = sgs.get(sgs.size() - 1);
        return Optional.of(new Indicator(code, LocalDate.parse(last.path("data").asText(), SGS_DATE),
                new BigDecimal(last.path("valor").asText())));
    }

    /** Median of the monthly rates: the "market" reference, less pulled by a few outliers than the mean. */
    public static BigDecimal median(List<BigDecimal> values) {
        if (values.isEmpty()) return null;
        List<BigDecimal> sorted = values.stream().sorted().toList();
        int middle = sorted.size() / 2;
        return sorted.size() % 2 == 1 ? sorted.get(middle)
                : sorted.get(middle - 1).add(sorted.get(middle)).divide(BigDecimal.valueOf(2), 4, java.math.RoundingMode.HALF_EVEN);
    }
}
