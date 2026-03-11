package com.unillm.client;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.unillm.config.ProviderProperties;
import com.unillm.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Google Gemini provider implementation.
 * Translates the unified AiRequest into Gemini's generateContent API format.
 *
 * Supports: gemini-pro, gemini-2.0-flash, gemini-1.5-pro, and any model starting with "gemini".
 */
@Component
public class GeminiClient implements AiClient {

    private static final Logger log = LoggerFactory.getLogger(GeminiClient.class);
    private static final String PROVIDER = "gemini";

    private final WebClient webClient;
    private final String apiKey;
    private final String defaultModel;
    private final ObjectMapper objectMapper;

    public GeminiClient(WebClient.Builder webClientBuilder,
                        ProviderProperties properties,
                        ObjectMapper objectMapper) {
        ProviderProperties.ProviderConfig config = properties.getGemini();
        this.webClient = webClientBuilder
                .baseUrl(config.getBaseUrl())
                .defaultHeader("Content-Type", "application/json")
                .build();
        this.apiKey = config.getApiKey();
        this.defaultModel = config.getDefaultModel();
        this.objectMapper = objectMapper;
    }

    @Override
    public String getProvider() {
        return PROVIDER;
    }

    @Override
    public boolean supports(String model) {
        return model != null && model.toLowerCase().startsWith("gemini");
    }

    // ─── Non-Streaming ────────────────────────────────────────────

    @Override
    public Mono<AiResponse> generate(AiRequest request) {
        String model = request.model() != null ? request.model() : defaultModel;
        Map<String, Object> body = toGeminiBody(request);

        return webClient.post()
                .uri("/models/{model}:generateContent?key={key}", model, apiKey)
                .bodyValue(body)
                .retrieve()
                .bodyToMono(JsonNode.class)
                .map(json -> parseResponse(json, model));
    }

    private AiResponse parseResponse(JsonNode json, String model) {
        String content = json.path("candidates").get(0)
                .path("content").path("parts").get(0)
                .path("text").asText();

        AiResponse.Usage usage = null;
        JsonNode usageNode = json.path("usageMetadata");
        if (!usageNode.isMissingNode()) {
            usage = new AiResponse.Usage(
                    usageNode.path("promptTokenCount").asInt(),
                    usageNode.path("candidatesTokenCount").asInt(),
                    usageNode.path("totalTokenCount").asInt()
            );
        }

        return new AiResponse(
                UUID.randomUUID().toString(),
                PROVIDER,
                model,
                List.of(AiMessage.assistant(content)),
                usage,
                System.currentTimeMillis()
        );
    }

    // ─── Streaming (SSE) ──────────────────────────────────────────

    @Override
    public Flux<AiStreamChunk> generateStream(AiRequest request) {
        String model = request.model() != null ? request.model() : defaultModel;
        Map<String, Object> body = toGeminiBody(request);

        return webClient.post()
                .uri("/models/{model}:streamGenerateContent?alt=sse&key={key}", model, apiKey)
                .bodyValue(body)
                .retrieve()
                .bodyToFlux(String.class)
                .filter(line -> !line.isBlank())
                .mapNotNull(line -> {
                    try {
                        String data = line.startsWith("data: ") ? line.substring(6) : line;
                        JsonNode node = objectMapper.readTree(data);
                        JsonNode candidates = node.path("candidates");

                        if (candidates.isEmpty()) return null;

                        String text = candidates.get(0)
                                .path("content").path("parts").get(0)
                                .path("text").asText("");

                        if (text.isEmpty()) return null;
                        return AiStreamChunk.text(PROVIDER, model, text);
                    } catch (Exception e) {
                        log.debug("Skipping unparseable Gemini SSE line: {}", line);
                        return null;
                    }
                })
                .concatWith(Flux.just(AiStreamChunk.done(PROVIDER, model)));
    }

    // ─── Internal DTO ─────────────────────────────────────────────

    private Map<String, Object> toGeminiBody(AiRequest request) {
        Map<String, Object> body = new LinkedHashMap<>();

        // Convert messages to Gemini's 'contents' format
        List<Map<String, Object>> contents = request.messages().stream()
                .filter(m -> !"system".equals(m.role()))
                .map(m -> {
                    Map<String, Object> content = new LinkedHashMap<>();
                    content.put("role", "user".equals(m.role()) ? "user" : "model");
                    content.put("parts", List.of(Map.of("text", m.content())));
                    return content;
                })
                .collect(Collectors.toList());
        body.put("contents", contents);

        // Handle system instruction
        String systemText = request.systemInstruction();
        if (systemText == null || systemText.isBlank()) {
            // Check if there's a system message in the messages list
            systemText = request.messages().stream()
                    .filter(m -> "system".equals(m.role()))
                    .map(AiMessage::content)
                    .findFirst().orElse(null);
        }
        if (systemText != null && !systemText.isBlank()) {
            body.put("systemInstruction", Map.of(
                    "parts", List.of(Map.of("text", systemText))
            ));
        }

        // Generation config
        Map<String, Object> genConfig = new LinkedHashMap<>();
        if (request.temperature() != null) genConfig.put("temperature", request.temperature());
        if (request.maxTokens() != null) genConfig.put("maxOutputTokens", request.maxTokens());
        if (!genConfig.isEmpty()) body.put("generationConfig", genConfig);

        return body;
    }
}
