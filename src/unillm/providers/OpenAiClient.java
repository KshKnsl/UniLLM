package unillm.providers;

import unillm.ChatMessage;
import unillm.ChatRequest;
import unillm.ChatResponse;
import unillm.ProviderClient;
import unillm.core.HttpJson;

import java.io.IOException;
import java.net.http.HttpClient;
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
        StringBuilder body = new StringBuilder();
        body.append("{\"model\":").append(HttpJson.quote(request.model())).append(",\"messages\":[");
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

        String res = HttpJson.post(http, "https://api.openai.com/v1/chat/completions",
                Map.of("Authorization", "Bearer " + apiKey), body.toString());

        String text = HttpJson.firstGroup(res,
                "\\\"message\\\"\\s*:\\s*\\{.*?\\\"content\\\"\\s*:\\s*\\\"((?:\\\\\\\"|\\\\\\\\|[^\\\"])+)\\\"");
        return new ChatResponse(name(), request.model(), text);
    }
}
