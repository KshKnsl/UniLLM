package unillm.providers;

import java.io.IOException;
import java.net.http.HttpClient;
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
        StringBuilder body = new StringBuilder();
        body.append("{\"model\":").append(HttpJson.quote(request.model()));
        body.append(",\"messages\":[");
        boolean first = true;
        for (ChatMessage m : request.messages()) {
            if ("system".equalsIgnoreCase(m.role()))
                continue;
            if (!first)
                body.append(',');
            String role = "assistant".equalsIgnoreCase(m.role()) ? "assistant" : "user";
            body.append("{\"role\":").append(HttpJson.quote(role)).append(",\"content\":")
                    .append(HttpJson.quote(m.content())).append("}");
            first = false;
        }
        body.append(']');
        body.append(",\"max_tokens\":").append(request.maxTokens() == null ? 1024 : request.maxTokens());
        if (request.temperature() != null)
            body.append(",\"temperature\":").append(request.temperature());
        if (request.system() != null && !request.system().trim().isEmpty()) {
            body.append(",\"system\":").append(HttpJson.quote(request.system()));
        }
        body.append('}');

        String res = HttpJson.post(http, "https://api.anthropic.com/v1/messages",
                Map.of("x-api-key", apiKey, "anthropic-version", "2023-06-01"), body.toString());

        String text = HttpJson.firstGroup(res, "\\\"text\\\"\\s*:\\s*\\\"((?:\\\\\\\"|\\\\\\\\|[^\\\"])+)\\\"");
        return new ChatResponse(name(), request.model(), text);
    }

    @Override
    public List<String> listModels() throws IOException, InterruptedException {
        String res = HttpJson.get(http, "https://api.anthropic.com/v1/models",
                Map.of("x-api-key", apiKey, "anthropic-version", "2023-06-01"));
        return HttpJson.allGroups(res, "\\\"id\\\"\\s*:\\s*\\\"((?:\\\\\\\"|\\\\\\\\|[^\\\"])+)\\\"");
    }
}
