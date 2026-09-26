package com.github.yildizmy.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * @param termsVersion version of the consent text the person read (frontend/src/consentTerms.json); required to authorise
 * @param language     language of that text, "pt" or "en"; required to authorise
 */
public record OpenFinanceConsentDecision(
        @NotBlank @Size(max = 100) String consentId,
        @Size(max = 20) String termsVersion,
        @Size(max = 5) String language) {
}
