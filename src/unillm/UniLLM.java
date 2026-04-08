package unillm;

import unillm.providers.ClaudeClient;
import unillm.providers.GeminiClient;
import unillm.providers.GroqClient;
import unillm.providers.OllamaClient;
import unillm.providers.OpenAiClient;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

public class UniLLM {
    private final List<ProviderClient> providers;

    public UniLLM(List<ProviderClient> providers) {
        this.providers = Collections.unmodifiableList(new ArrayList<ProviderClient>(providers));
    }

    public UniLLM(ProviderClient... providers) {
        this(Arrays.asList(providers));
    }

    public static UniLLM defaultClients(String openAiKey, String claudeKey, String geminiKey,
                                        String groqKey, boolean includeOllama) {
        List<ProviderClient> clients = new ArrayList<>();
        if (openAiKey != null && !openAiKey.isBlank()) clients.add(new OpenAiClient(openAiKey));
        if (claudeKey != null && !claudeKey.isBlank()) clients.add(new ClaudeClient(claudeKey));
        if (geminiKey != null && !geminiKey.isBlank()) clients.add(new GeminiClient(geminiKey));
        if (groqKey != null && !groqKey.isBlank()) clients.add(new GroqClient(groqKey));
        if (includeOllama) clients.add(new OllamaClient("http://localhost:11434"));
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

    public List<String> listModels(String providerName) throws IOException, InterruptedException {
        return providers.stream()
                .filter(p -> p.name().equalsIgnoreCase(providerName))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Provider not configured: " + providerName))
                .listModels();
    }

    public Map<String, List<String>> listAllModels() throws IOException, InterruptedException {
        Map<String, List<String>> out = new LinkedHashMap<>();
        for (ProviderClient provider : providers) {
            out.put(provider.name(), provider.listModels());
        }
        return out;
    }

    private ProviderClient resolve(String model) {
        return providers.stream()
                .filter(p -> p.supports(model))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("No provider for model: " + model));
    }
}
