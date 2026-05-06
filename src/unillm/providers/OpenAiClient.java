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

public class OpenAiClient implements ProviderClient {
    private final String apiKey;
    private final HttpClient http = HttpClient.newHttpClient();

    public OpenAiClient(String apiKey) {
        this.apiKey = apiKey;
    }

    @Override
    public String name() {
        return "openai";
    }

    @Override
    public boolean supports(String model) {
        return model != null && model.toLowerCase().startsWith("gpt");
    }

    @Override
    public ChatResponse chat(ChatRequest request) throws IOException, InterruptedException {
        ObjectNode body = HttpJson.MAPPER.createObjectNode();
        body.put("model", request.model());
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

        if (request.temperature() != null)
            body.put("temperature", request.temperature());
        if (request.maxTokens() != null)
            body.put("max_tokens", request.maxTokens());

        JsonNode res = HttpJson.postJson(http, "https://api.openai.com/v1/chat/completions",
                Map.of("Authorization", "Bearer " + apiKey), body);

        String text = res.path("choices").path(0).path("message").path("content").asText("");
        return new ChatResponse(name(), request.model(), text);
    }

    @Override
    public List<String> listModels() throws IOException, InterruptedException {
        JsonNode res = HttpJson.getJson(http, "https://api.openai.com/v1/models",
                Map.of("Authorization", "Bearer " + apiKey));
        List<String> models = new java.util.ArrayList<>();
        for (JsonNode item : res.path("data")) {
            String id = item.path("id").asText("");
            if (!id.isEmpty()) {
                models.add(id);
            }
        }
        return models;
    }
}
