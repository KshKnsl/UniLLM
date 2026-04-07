package unillm.providers;

import unillm.ChatMessage;
import unillm.ChatRequest;
import unillm.ChatResponse;
import unillm.ProviderClient;
import unillm.core.HttpJson;

import java.io.IOException;
import java.net.http.HttpClient;
import java.util.Map;

public class GeminiClient implements ProviderClient {
    private final String apiKey;
    private final HttpClient http = HttpClient.newHttpClient();

    public GeminiClient(String apiKey) {
        this.apiKey = apiKey;
    }

    @Override
    public String name() {
        return "gemini";
    }

    @Override
    public boolean supports(String model) {
        return model != null && model.toLowerCase().startsWith("gemini");
    }

    @Override
    public ChatResponse chat(ChatRequest request) throws IOException, InterruptedException {
        StringBuilder body = new StringBuilder();
        body.append("{\"contents\":[");
        boolean first = true;
        for (ChatMessage m : request.messages()) {
            if ("system".equalsIgnoreCase(m.role()))
                continue;
            if (!first)
                body.append(',');
            String role = "assistant".equalsIgnoreCase(m.role()) ? "model" : "user";
            body.append("{\"role\":").append(HttpJson.quote(role))
                    .append(",\"parts\":[{\"text\":").append(HttpJson.quote(m.content())).append("}]}");
            first = false;
        }
        body.append(']');
        if (request.system() != null && !request.system().trim().isEmpty()) {
            body.append(",\"systemInstruction\":{\"parts\":[{\"text\":")
                    .append(HttpJson.quote(request.system())).append("}]}");
        }
        if (request.temperature() != null || request.maxTokens() != null) {
            body.append(",\"generationConfig\":{");
            boolean cfgFirst = true;
            if (request.temperature() != null) {
                body.append("\"temperature\":").append(request.temperature());
                cfgFirst = false;
            }
            if (request.maxTokens() != null) {
                if (!cfgFirst)
                    body.append(',');
                body.append("\"maxOutputTokens\":").append(request.maxTokens());
            }
            body.append('}');
        }
        body.append('}');

        String url = "https://generativelanguage.googleapis.com/v1beta/models/" + request.model()
                + ":generateContent?key=" + apiKey;
        String res = HttpJson.post(http, url, Map.of(), body.toString());
        String text = HttpJson.firstGroup(res, "\\\"text\\\"\\s*:\\s*\\\"((?:\\\\\\\"|\\\\\\\\|[^\\\"])+)\\\"");
        return new ChatResponse(name(), request.model(), text);
    }
}
