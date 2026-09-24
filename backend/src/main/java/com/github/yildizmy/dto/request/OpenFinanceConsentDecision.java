package com.github.yildizmy.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record OpenFinanceConsentDecision(@NotBlank @Size(max = 100) String consentId) {
}
