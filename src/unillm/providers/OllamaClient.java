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

public class OllamaClient implements ProviderClient {
    private final String baseUrl;
    private final HttpClient http = HttpClient.newHttpClient();

    public OllamaClient(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    @Override
    public String name() {
        return "ollama";
    }

    @Override
    public boolean supports(String model) {
        return model != null && model.toLowerCase().startsWith("ollama/");
    }

    @Override
    public ChatResponse chat(ChatRequest request) throws IOException, InterruptedException {
        String model = request.model().substring("ollama/".length());

        ObjectNode body = HttpJson.MAPPER.createObjectNode();
        body.put("model", model);
        ArrayNode messages = body.putArray("messages");

        if (request.system() != null && !request.system().trim().isEmpty()) {
            ObjectNode systemMessage = messages.addObject();
            systemMessage.put("role", "system");
            systemMessage.put("content", request.system());
        }
        for (ChatMessage m : request.messages()) {
            ObjectNode message = messages.addObject();
            message.put("role", m.role());
            message.put("content", m.content());
        }

        body.put("stream", false);
        if (request.temperature() != null || request.maxTokens() != null) {
            ObjectNode options = body.putObject("options");
            if (request.temperature() != null) {
                options.put("temperature", request.temperature());
            }
            if (request.maxTokens() != null) {
                options.put("num_predict", request.maxTokens());
            }
        }

        JsonNode res = HttpJson.postJson(http, baseUrl + "/api/chat", Map.of(), body);
        String text = res.path("message").path("content").asText("");
        return new ChatResponse(name(), request.model(), text);
    }

    @Override
    public List<String> listModels() throws IOException, InterruptedException {
        JsonNode res = HttpJson.getJson(http, baseUrl + "/api/tags", Map.of());
        List<String> models = new java.util.ArrayList<>();
        for (JsonNode item : res.path("models")) {
            String name = item.path("name").asText("");
            if (!name.isEmpty()) {
                models.add(name);
            }
        }
        return models;
    }
}
