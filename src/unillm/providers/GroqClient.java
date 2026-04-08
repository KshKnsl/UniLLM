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

public class GroqClient implements ProviderClient {
    private final String apiKey;
    private final HttpClient http = HttpClient.newHttpClient();

    public GroqClient(String apiKey) {
        this.apiKey = apiKey;
    }

    @Override
    public String name() {
        return "groq";
    }

    @Override
    public boolean supports(String model) {
        return model != null && model.toLowerCase().startsWith("groq/");
    }

    @Override
    public ChatResponse chat(ChatRequest request) throws IOException, InterruptedException {
        String model = request.model().substring("groq/".length());

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
            body.append("{\"role\":").append(HttpJson.quote(m.role())).append(",\"content\":")
                    .append(HttpJson.quote(m.content())).append("}");
            first = false;
        }
        body.append(']');
        if (request.temperature() != null)
            body.append(",\"temperature\":").append(request.temperature());
        if (request.maxTokens() != null)
            body.append(",\"max_tokens\":").append(request.maxTokens());
        body.append('}');

        String res = HttpJson.post(http, "https://api.groq.com/openai/v1/chat/completions",
                Map.of("Authorization", "Bearer " + apiKey), body.toString());

        String text = HttpJson.firstGroup(res,
                "\\\"message\\\"\\s*:\\s*\\{.*?\\\"content\\\"\\s*:\\s*\\\"((?:\\\\\\\"|\\\\\\\\|[^\\\"])+)\\\"");
        return new ChatResponse(name(), request.model(), text);
    }

    @Override
    public List<String> listModels() throws IOException, InterruptedException {
        String res = HttpJson.get(http, "https://api.groq.com/openai/v1/models",
                Map.of("Authorization", "Bearer " + apiKey));
        List<String> ids = HttpJson.allGroups(res, "\\\"id\\\"\\s*:\\s*\\\"((?:\\\\\\\"|\\\\\\\\|[^\\\"])+)\\\"");
        for (int i = 0; i < ids.size(); i++) {
            ids.set(i, "groq/" + ids.get(i));
        }
        return ids;
    }
}
