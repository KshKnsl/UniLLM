package unillm.providers;

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

        StringBuilder body = new StringBuilder();
        body.append("{\"model\":").append(HttpJson.quote(model)).append(",\"messages\":[");
        boolean first = true;
        if (request.system() != null && !request.system().trim().isEmpty()) {
            body.append("{\"role\":\"system\",\"content\":").append(HttpJson.quote(request.system())).append("}");
            first = false;
        }
        for (ChatMessage m : request.messages()) {
            if (!first)
                body.append(',');
            body.append("{\"role\":").append(HttpJson.quote(m.role()))
                    .append(",\"content\":").append(HttpJson.quote(m.content())).append("}");
            first = false;
        }
        body.append("],\"stream\":false");
        if (request.temperature() != null || request.maxTokens() != null) {
            body.append(",\"options\":{");
            boolean optFirst = true;
            if (request.temperature() != null) {
                body.append("\"temperature\":").append(request.temperature());
                optFirst = false;
            }
            if (request.maxTokens() != null) {
                if (!optFirst)
                    body.append(',');
                body.append("\"num_predict\":").append(request.maxTokens());
            }
            body.append('}');
        }
        body.append('}');

        String res = HttpJson.post(http, baseUrl + "/api/chat", Map.of(), body.toString());
        String text = HttpJson.firstGroup(res,
                "\\\"message\\\"\\s*:\\s*\\{.*?\\\"content\\\"\\s*:\\s*\\\"((?:\\\\\\\"|\\\\\\\\|[^\\\"])+)\\\"");
        return new ChatResponse(name(), request.model(), text);
    }

    @Override
    public List<String> listModels() throws IOException, InterruptedException {
        String res = HttpJson.get(http, baseUrl + "/api/tags", Map.of());
        return HttpJson.allGroups(res, "\\\"name\\\"\\s*:\\s*\\\"((?:\\\\\\\"|\\\\\\\\|[^\\\"])+)\\\"");
    }
}
