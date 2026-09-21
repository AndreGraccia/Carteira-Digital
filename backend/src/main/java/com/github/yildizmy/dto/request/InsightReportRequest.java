package com.github.yildizmy.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

public record InsightReportRequest(
        @NotNull @Min(1) @Max(12) Integer months,
        @NotNull @Pattern(regexp = "^(pt-BR|en)$") String language
) {
}
