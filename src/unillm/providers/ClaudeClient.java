package unillm.providers;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.io.IOException;
import java.net.http.HttpClient;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import unillm.ChatMessage;
import unillm.ChatRequest;
import unillm.ChatResponse;
import unillm.ProviderClient;
import unillm.core.HttpJson;

public class ClaudeClient implements ProviderClient {
    private final String apiKey;
    private final HttpClient http = HttpClient.newHttpClient();

    public ClaudeClient(String apiKey) {
        this.apiKey = apiKey;
    }

    @Override
    public String name() {
        return "claude";
    }

    @Override
    public boolean supports(String model) {
        return model != null && model.toLowerCase().startsWith("claude");
    }

    @Override
    public ChatResponse chat(ChatRequest request) throws IOException, InterruptedException {
        ObjectNode body = HttpJson.MAPPER.createObjectNode();
        body.put("model", request.model());
        ArrayNode messages = body.putArray("messages");

        for (ChatMessage m : request.messages()) {
            if ("system".equalsIgnoreCase(m.role()))
                continue;

            String role = "assistant".equalsIgnoreCase(m.role()) ? "assistant" : "user";
            ObjectNode msg = messages.addObject();
            msg.put("role", role);
            msg.put("content", m.content());
        }

        Integer requestedMaxTokens = request.maxTokens();
        body.put("max_tokens", requestedMaxTokens != null ? requestedMaxTokens.intValue() : 1024);
        if (request.temperature() != null)
            body.put("temperature", request.temperature());
        if (request.system() != null && !request.system().trim().isEmpty()) {
            body.put("system", request.system());
        }

        JsonNode res = HttpJson.postJson(http, "https://api.anthropic.com/v1/messages",
                Map.of("x-api-key", apiKey, "anthropic-version", "2023-06-01"), body);

        String text = "";
        for (JsonNode block : res.path("content")) {
            if ("text".equals(block.path("type").asText(""))) {
                text = block.path("text").asText("");
                break;
            }
        }
        return new ChatResponse(name(), request.model(), text);
    }

    @Override
    public List<String> listModels() throws IOException, InterruptedException {
        JsonNode res = HttpJson.getJson(http, "https://api.anthropic.com/v1/models",
                Map.of("x-api-key", apiKey, "anthropic-version", "2023-06-01"));
        List<String> models = new ArrayList<>();
        for (JsonNode item : res.path("data")) {
            String id = item.path("id").asText("");
            if (!id.isEmpty()) {
                models.add(id);
            }
        }
        return models;
    }
}
