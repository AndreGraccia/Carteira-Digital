package com.github.yildizmy.service.insight;

import java.math.BigDecimal;
import java.util.List;

public record FinancialSnapshot(
        int periodMonths,
        BigDecimal inflow,
        BigDecimal outflow,
        BigDecimal internalTransfers,
        BigDecimal netFlow,
        long transactionCount,
        List<MonthlyAggregate> monthlyFlow
) {
    public record MonthlyAggregate(String month, BigDecimal inflow, BigDecimal outflow) {
    }
}
