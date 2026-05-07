package unillm.providers;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import unillm.ChatMessage;
import unillm.ChatRequest;
import unillm.ChatResponse;
import unillm.ProviderClient;
import unillm.core.HttpJson;

import java.io.IOException;
import java.net.http.HttpClient;
import java.util.List;
import java.util.Map;

public class GeminiClient implements ProviderClient {
    public static final String DEFAULT_BASE_URL = "https://jiitproxy.jportal696.workers.dev/api";

    private final String apiKey;
    private final String baseUrl;
    private final HttpClient http = HttpClient.newHttpClient();

    public GeminiClient(String apiKey) {
        this(apiKey, DEFAULT_BASE_URL);
    }

    public GeminiClient(String apiKey, String baseUrl) {
        this.apiKey = apiKey;
        this.baseUrl = normalizeBaseUrl(baseUrl);
    }

    @Override
    public String name() {
        return "gemini";
    }

    @Override
    public boolean supports(String model) {
        if (model == null) {
            return false;
        }
        String lower = model.toLowerCase();
        return lower.startsWith("gemini") || lower.startsWith("models/gemini");
    }

    @Override
    public ChatResponse chat(ChatRequest request) throws IOException, InterruptedException {
        ObjectNode body = HttpJson.MAPPER.createObjectNode();
        ArrayNode contents = body.putArray("contents");

        for (ChatMessage m : request.messages()) {
            if ("system".equalsIgnoreCase(m.role()))
                continue;

            String role = "assistant".equalsIgnoreCase(m.role()) ? "model" : "user";
            ObjectNode content = contents.addObject();
            content.put("role", role);
            ArrayNode parts = content.putArray("parts");
            parts.addObject().put("text", m.content());
        }

        if (request.system() != null && !request.system().trim().isEmpty()) {
            ObjectNode instruction = body.putObject("systemInstruction");
            instruction.putArray("parts").addObject().put("text", request.system());
        }
        if (request.temperature() != null || request.maxTokens() != null) {
            ObjectNode config = body.putObject("generationConfig");
            if (request.temperature() != null) {
                config.put("temperature", request.temperature());
            }
            if (request.maxTokens() != null) {
                config.put("maxOutputTokens", request.maxTokens());
            }
        }

        String url = baseUrl + "/v1beta/models/" + normalizeModel(request.model())
                + ":generateContent?key=" + apiKey;
        JsonNode res = HttpJson.postJson(http, url, Map.of(), body);
        String text = res.path("candidates").path(0).path("content").path("parts").path(0).path("text").asText("");
        return new ChatResponse(name(), request.model(), text);
    }

    @Override
    public List<String> listModels() throws IOException, InterruptedException {
        String url = baseUrl + "/v1beta/models?key=" + apiKey;
        JsonNode res = HttpJson.getJson(http, url, Map.of());
        List<String> names = new java.util.ArrayList<>();
        for (JsonNode item : res.path("models")) {
            String name = item.path("name").asText("");
            if (name.startsWith("models/")) {
                names.add(name.substring("models/".length()));
            }
        }
        return names;
    }

    private String normalizeModel(String model) {
        return model.startsWith("models/") ? model.substring("models/".length()) : model;
    }

    private String normalizeBaseUrl(String value) {
        if (value == null || value.isBlank()) {
            return DEFAULT_BASE_URL;
        }
        String trimmed = value.trim();
        return trimmed.endsWith("/") ? trimmed.substring(0, trimmed.length() - 1) : trimmed;
    }
}
