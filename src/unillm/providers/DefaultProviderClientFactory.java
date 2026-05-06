package unillm.providers;

import java.util.ArrayList;
import java.util.List;

import unillm.ProviderClient;
import unillm.ProviderClientFactory;

public class DefaultProviderClientFactory implements ProviderClientFactory {
    private final String openAiKey;
    private final String claudeKey;
    private final String geminiKey;
    private final String groqKey;
    private final boolean includeOllama;
    private final String ollamaBaseUrl;

    public DefaultProviderClientFactory(String openAiKey, String claudeKey, String geminiKey,
                                        String groqKey, boolean includeOllama) {
        this(openAiKey, claudeKey, geminiKey, groqKey, includeOllama, "http://localhost:11434");
    }

    public DefaultProviderClientFactory(String openAiKey, String claudeKey, String geminiKey,
                                        String groqKey, boolean includeOllama, String ollamaBaseUrl) {
        this.openAiKey = openAiKey;
        this.claudeKey = claudeKey;
        this.geminiKey = geminiKey;
        this.groqKey = groqKey;
        this.includeOllama = includeOllama;
        this.ollamaBaseUrl = ollamaBaseUrl;
    }

    @Override
    public List<ProviderClient> createClients() {
        List<ProviderClient> clients = new ArrayList<>();
        if (hasText(openAiKey)) clients.add(new OpenAiClient(openAiKey));
        if (hasText(claudeKey)) clients.add(new ClaudeClient(claudeKey));
        if (hasText(geminiKey)) clients.add(new GeminiClient(geminiKey));
        if (hasText(groqKey)) clients.add(new GroqClient(groqKey));
        if (includeOllama) clients.add(new OllamaClient(ollamaBaseUrl));
        return clients;
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}