package com.github.yildizmy.service.insight;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.ObjectNode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Map;
import java.util.Set;

@Component
@lombok.extern.slf4j.Slf4j
public class GeminiInsightNarrativeGenerator implements InsightNarrativeGenerator {
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;
    private final String apiKey;
    private final String model;
    private final URI endpoint;
    private final boolean searchGroundingEnabled;

    @org.springframework.beans.factory.annotation.Autowired
    public GeminiInsightNarrativeGenerator(
            ObjectMapper objectMapper,
            @Value("${app.ai.geminiApiKey:}") String apiKey,
            @Value("${app.ai.geminiModel:gemini-3.8-flash}") String model,
            @Value("${app.ai.endpoint}") String endpoint,
            @Value("${app.ai.searchGroundingEnabled:false}") boolean searchGroundingEnabled
    ) {
        this(objectMapper, apiKey, model, endpoint, searchGroundingEnabled,
                HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(8)).build(), Thread::sleep);
    }

    GeminiInsightNarrativeGenerator(ObjectMapper objectMapper, String apiKey, String model, String endpoint,
                                    boolean searchGroundingEnabled, HttpClient httpClient, Sleeper sleeper) {
        this.objectMapper = objectMapper;
        this.apiKey = apiKey;
        this.model = model;
        this.endpoint = URI.create(endpoint);
        this.searchGroundingEnabled = searchGroundingEnabled;
        this.httpClient = httpClient;
        this.sleeper = sleeper;
    }

    /** Attempts in total for a rate limit (429) or a server error (5xx); other errors are not retried. */
    static final int MAX_ATTEMPTS = 3;

    interface Sleeper {
        void sleep(long millis) throws InterruptedException;
    }

    private final Sleeper sleeper;

    /**
     * Sends with retries on 429 and 5xx: Retry-After when the API sends it (capped at 8 s), otherwise 1 s, 2 s with
     * jitter. The status and model are logged, never the prompt or the answer.
     */
    private HttpResponse<String> send(HttpRequest request) throws Exception {
        for (int attempt = 1; ; attempt++) {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            int status = response.statusCode();
            if (status >= 200 && status < 300) return response;
            boolean retryable = status == 429 || status >= 500;
            log.warn("Gemini returned HTTP {} for model {} (attempt {}/{})", status, model, attempt, MAX_ATTEMPTS);
            if (!retryable || attempt == MAX_ATTEMPTS) {
                throw new IllegalStateException("Gemini request failed with status " + status);
            }
            long wait = response.headers().firstValueAsLong("Retry-After").orElse(-1) * 1000;
            if (wait <= 0) wait = 1000L * (1L << (attempt - 1)) + java.util.concurrent.ThreadLocalRandom.current().nextLong(250);
            sleeper.sleep(Math.min(wait, 8000));
        }
    }

    @Override
    public boolean isEnabled() {
        return StringUtils.hasText(apiKey);
    }

    @Override
    public Optional<GeneratedNarrative> generate(
            FinancialSnapshot snapshot,
            String language,
            Map<String, String> researchContext
    ) {
        if (!isEnabled()) {
            return Optional.empty();
        }

        try {
            ObjectNode requestBody = objectMapper.createObjectNode();
            requestBody.put("model", model);
            requestBody.put("store", false);
            requestBody.put("input", buildPrompt(snapshot, language, researchContext));
            requestBody.set("response_format", responseFormat());
            if (searchGroundingEnabled) {
                requestBody.putArray("tools").addObject().put("type", "google_search");
            }

            HttpRequest request = HttpRequest.newBuilder(endpoint)
                    .timeout(Duration.ofSeconds(15))
                    .header("Content-Type", "application/json")
                    .header("x-goog-api-key", apiKey)
                    .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(requestBody)))
                    .build();

            HttpResponse<String> response = send(request);
            return Optional.of(parseNarrative(response.body(), researchContext.keySet()));
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Gemini request interrupted", exception);
        } catch (Exception exception) {
            throw new IllegalStateException("Gemini report generation failed", exception);
        }
    }

    private String buildPrompt(
            FinancialSnapshot snapshot,
            String language,
            Map<String, String> researchContext
    ) throws Exception {
        String locale = "en".equals(language) ? "English" : "Brazilian Portuguese";
        return """
                You are a personal cash-flow analyst. Write in %s using a calm, concise and professional tone.
                Analyze only the aggregate JSON below. It contains no transaction descriptions or personal data.
                Do not infer spending categories, income stability, debt, investments, age or identity.
                Do not provide investment, credit, legal or tax advice. Never promise outcomes.
                Use the research catalog below to support general educational statements. If Google Search is available,
                use it only to verify these sources and current general context. Never use search results to recommend a
                specific financial product. Each observation must cite zero or more IDs from the catalog only.
                Recommend small, measurable actions based on cash flow. Return only the requested JSON.

                Research catalog:
                %s

                Aggregate cash-flow data:
                %s
                """.formatted(locale, objectMapper.writeValueAsString(researchContext),
                objectMapper.writeValueAsString(snapshot));
    }

    private ObjectNode responseFormat() {
        ObjectNode item = objectMapper.createObjectNode();
        item.put("type", "object");
        ObjectNode itemProperties = item.putObject("properties");
        itemProperties.putObject("title").put("type", "string");
        itemProperties.putObject("body").put("type", "string");
        itemProperties.putObject("tone").put("type", "string")
                .putArray("enum").add("neutral").add("positive").add("attention");
        ObjectNode citations = itemProperties.putObject("citationIds");
        citations.put("type", "array");
        citations.putObject("items").put("type", "string");
        item.putArray("required").add("title").add("body").add("tone").add("citationIds");

        ObjectNode schema = objectMapper.createObjectNode();
        schema.put("type", "object");
        ObjectNode properties = schema.putObject("properties");
        properties.putObject("headline").put("type", "string");
        properties.putObject("summary").put("type", "string");
        properties.putObject("observations").put("type", "array").set("items", item);
        properties.putObject("recommendedActions").put("type", "array").set("items", item.deepCopy());
        schema.putArray("required").add("headline").add("summary").add("observations").add("recommendedActions");

        ObjectNode format = objectMapper.createObjectNode();
        format.put("type", "text");
        format.put("mime_type", "application/json");
        format.set("schema", schema);
        return format;
    }

    private GeneratedNarrative parseNarrative(String responseBody, Set<String> allowedCitationIds) throws Exception {
        JsonNode response = objectMapper.readTree(responseBody);
        JsonNode output = response.path("output_text");
        if (output.isMissingNode()) {
            output = response.path("interaction").path("output_text");
        }
        JsonNode narrative = output.isTextual() ? objectMapper.readTree(output.asText()) : response;

        return new GeneratedNarrative(
                limited(narrative.path("headline").asText(), 120),
                limited(narrative.path("summary").asText(), 600),
                parseItems(narrative.path("observations"), allowedCitationIds),
                parseItems(narrative.path("recommendedActions"), allowedCitationIds)
        );
    }

    private List<GeneratedNarrative.NarrativeItem> parseItems(JsonNode nodes, Set<String> allowedCitationIds) {
        List<GeneratedNarrative.NarrativeItem> items = new ArrayList<>();
        if (!(nodes instanceof ArrayNode array)) {
            return items;
        }
        for (JsonNode node : array) {
            if (items.size() == 4) break;
            String title = limited(node.path("title").asText(), 100);
            String body = limited(node.path("body").asText(), 420);
            if (!StringUtils.hasText(title) || !StringUtils.hasText(body)) continue;
            String tone = Set.of("neutral", "positive", "attention").contains(node.path("tone").asText())
                    ? node.path("tone").asText() : "neutral";
            List<String> citations = new ArrayList<>();
            node.path("citationIds").forEach(value -> {
                if (value.isTextual() && allowedCitationIds.contains(value.asText()) && !citations.contains(value.asText())) {
                    citations.add(value.asText());
                }
            });
            items.add(new GeneratedNarrative.NarrativeItem(title, body, tone, List.copyOf(citations)));
        }
        return List.copyOf(items);
    }

    private String limited(String value, int maxLength) {
        if (value == null) return "";
        String normalized = value.strip();
        return normalized.length() <= maxLength ? normalized : normalized.substring(0, maxLength).strip();
    }
}
