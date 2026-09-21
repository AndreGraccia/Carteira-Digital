package com.github.yildizmy.service.insight;

import java.util.Optional;
import java.util.Map;

public interface InsightNarrativeGenerator {
    boolean isEnabled();

    Optional<GeneratedNarrative> generate(
            FinancialSnapshot snapshot,
            String language,
            Map<String, String> researchContext
    );
}
