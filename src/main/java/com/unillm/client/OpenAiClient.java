package com.unillm.client;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.unillm.config.ProviderProperties;
import com.unillm.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.stream.Collectors;

/**
 * OpenAI provider implementation.
 * Translates the unified AiRequest into OpenAI's Chat Completions API format.
 *
 * Supports: gpt-4o, gpt-4, gpt-3.5-turbo, and any model name starting with "gpt".
 */
@Component
public class OpenAiClient implements AiClient {

    private static final Logger log = LoggerFactory.getLogger(OpenAiClient.class);
    private static final String PROVIDER = "openai";

    private final WebClient webClient;
    private final String defaultModel;
    private final ObjectMapper objectMapper;

    public OpenAiClient(WebClient.Builder webClientBuilder,
                        ProviderProperties properties,
                        ObjectMapper objectMapper) {
        ProviderProperties.ProviderConfig config = properties.getOpenai();
        this.webClient = webClientBuilder
                .baseUrl(config.getBaseUrl())
                .defaultHeader("Authorization", "Bearer " + config.getApiKey())
                .defaultHeader("Content-Type", "application/json")
                .build();
        this.defaultModel = config.getDefaultModel();
        this.objectMapper = objectMapper;
    }

    @Override
    public String getProvider() {
        return PROVIDER;
    }

    @Override
    public boolean supports(String model) {
        return model != null && model.toLowerCase().startsWith("gpt");
    }

    // ─── Non-Streaming ────────────────────────────────────────────

    @Override
    public Mono<AiResponse> generate(AiRequest request) {
        String model = request.model() != null ? request.model() : defaultModel;
        OpenAiBody body = toOpenAiBody(request, model, false);

        return webClient.post()
                .uri("/chat/completions")
                .bodyValue(body)
                .retrieve()
                .bodyToMono(JsonNode.class)
                .map(json -> parseResponse(json, model));
    }

    private AiResponse parseResponse(JsonNode json, String model) {
        String content = json.path("choices").get(0)
                .path("message").path("content").asText();

        AiResponse.Usage usage = null;
        JsonNode usageNode = json.path("usage");
        if (!usageNode.isMissingNode()) {
            usage = new AiResponse.Usage(
                    usageNode.path("prompt_tokens").asInt(),
                    usageNode.path("completion_tokens").asInt(),
                    usageNode.path("total_tokens").asInt()
            );
        }

        return new AiResponse(
                json.path("id").asText(),
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
        OpenAiBody body = toOpenAiBody(request, model, true);

        return webClient.post()
                .uri("/chat/completions")
                .bodyValue(body)
                .accept(MediaType.TEXT_EVENT_STREAM)
                .retrieve()
                .bodyToFlux(String.class)
                .filter(line -> !line.isBlank() && !line.equals("[DONE]"))
                .mapNotNull(line -> {
                    try {
                        // OpenAI streams "data: {...}" lines
                        String data = line.startsWith("data: ") ? line.substring(6) : line;
                        if (data.equals("[DONE]")) return AiStreamChunk.done(PROVIDER, model);

                        JsonNode node = objectMapper.readTree(data);
                        String delta = node.path("choices").get(0)
                                .path("delta").path("content").asText("");

                        if (delta.isEmpty()) return null;
                        return AiStreamChunk.text(PROVIDER, model, delta);
                    } catch (Exception e) {
                        log.debug("Skipping unparseable SSE line: {}", line);
                        return null;
                    }
                })
                .concatWith(Flux.just(AiStreamChunk.done(PROVIDER, model)));
    }

    // ─── Internal DTO ─────────────────────────────────────────────

    private OpenAiBody toOpenAiBody(AiRequest request, String model, boolean stream) {
        List<OpenAiMsg> messages = request.messages().stream()
                .map(m -> new OpenAiMsg(m.role(), m.content()))
                .collect(Collectors.toList());

        // Prepend system instruction if present
        if (request.systemInstruction() != null && !request.systemInstruction().isBlank()) {
            messages.add(0, new OpenAiMsg("system", request.systemInstruction()));
        }

        return new OpenAiBody(model, messages, request.temperature(),
                request.maxTokens(), stream);
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    record OpenAiBody(
            String model,
            List<OpenAiMsg> messages,
            Double temperature,
            @JsonProperty("max_tokens") Integer maxTokens,
            boolean stream
    ) {}

    record OpenAiMsg(String role, String content) {}
}
