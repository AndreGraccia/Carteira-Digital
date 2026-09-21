package com.github.yildizmy.service.insight;

import java.util.List;

public record GeneratedNarrative(
        String headline,
        String summary,
        List<NarrativeItem> observations,
        List<NarrativeItem> recommendedActions
) {
    public record NarrativeItem(String title, String body, String tone, List<String> citationIds) {
    }
}
