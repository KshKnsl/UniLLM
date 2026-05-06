package unillm.providers;

import unillm.ProviderClient;
import unillm.ProviderClientFactory;

import java.util.ArrayList;
import java.util.List;

public class DefaultProviderClientFactory implements ProviderClientFactory {
    private final String openAiKey;
    private final String claudeKey;
    private final String geminiKey;
    private final String groqKey;
    private final boolean includeOllama;

    public DefaultProviderClientFactory(String openAiKey, String claudeKey, String geminiKey,
                                        String groqKey, boolean includeOllama) {
        this.openAiKey = openAiKey;
        this.claudeKey = claudeKey;
        this.geminiKey = geminiKey;
        this.groqKey = groqKey;
        this.includeOllama = includeOllama;
    }

    @Override
    public List<ProviderClient> createClients() {
        List<ProviderClient> clients = new ArrayList<>();
        if (hasText(openAiKey)) clients.add(new OpenAiClient(openAiKey));
        if (hasText(claudeKey)) clients.add(new ClaudeClient(claudeKey));
        if (hasText(geminiKey)) clients.add(new GeminiClient(geminiKey));
        if (hasText(groqKey)) clients.add(new GroqClient(groqKey));
        if (includeOllama) clients.add(new OllamaClient("http://localhost:11434"));
        return clients;
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}