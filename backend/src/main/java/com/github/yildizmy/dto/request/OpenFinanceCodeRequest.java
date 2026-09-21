package com.github.yildizmy.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record OpenFinanceCodeRequest(
        @NotBlank
        @Pattern(regexp = "\\d{8}")
        String code
) {}
