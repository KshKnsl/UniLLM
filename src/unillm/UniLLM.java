package unillm;

import unillm.providers.ClaudeClient;
import unillm.providers.GeminiClient;
import unillm.providers.OllamaClient;
import unillm.providers.OpenAiClient;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class UniLLM {
    private final List<ProviderClient> providers;

    public UniLLM(List<ProviderClient> providers) {
        this.providers = Collections.unmodifiableList(new ArrayList<ProviderClient>(providers));
    }

    public UniLLM(ProviderClient... providers) {
        this(Arrays.asList(providers));
    }

    public static UniLLM defaultClients(String openAiKey, String claudeKey, String geminiKey) {
        List<ProviderClient> clients = new ArrayList<>();
        if (openAiKey != null && !openAiKey.isBlank()) clients.add(new OpenAiClient(openAiKey));
        if (claudeKey != null && !claudeKey.isBlank()) clients.add(new ClaudeClient(claudeKey));
        if (geminiKey != null && !geminiKey.isBlank()) clients.add(new GeminiClient(geminiKey));
        clients.add(new OllamaClient("http://localhost:11434"));
        return new UniLLM(clients);
    }

    public List<String> providers() {
        return providers.stream().map(ProviderClient::name).collect(Collectors.toList());
    }

    public ChatResponse chat(ChatRequest request) throws IOException, InterruptedException {
        return resolve(request.model()).chat(request);
    }

    public ChatResponse chat(String model, String prompt) throws IOException, InterruptedException {
        return chat(ChatRequest.of(model, prompt));
    }

    private ProviderClient resolve(String model) {
        return providers.stream()
                .filter(p -> p.supports(model))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("No provider for model: " + model));
    }
}
