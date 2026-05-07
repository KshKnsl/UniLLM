package unillm.providers;

import java.util.ArrayList;
import java.util.List;

import unillm.ProviderClient;
import unillm.ProviderClientFactory;

public class DefaultProviderClientFactory implements ProviderClientFactory {
    private static final String DEFAULT_GEMINI_BASE_URL = GeminiClient.DEFAULT_BASE_URL;

    private final String openAiKey;
    private final String claudeKey;
    private final String geminiKey;
    private final String groqKey;
    private final boolean includeOllama;
    private final String ollamaBaseUrl;
    private final String geminiBaseUrl;

    public DefaultProviderClientFactory(String openAiKey, String claudeKey, String geminiKey,
                                        String groqKey, boolean includeOllama) {
        this(openAiKey, claudeKey, geminiKey, groqKey, includeOllama, "http://localhost:11434", DEFAULT_GEMINI_BASE_URL);
    }

    public DefaultProviderClientFactory(String openAiKey, String claudeKey, String geminiKey,
                                        String groqKey, boolean includeOllama, String ollamaBaseUrl) {
        this(openAiKey, claudeKey, geminiKey, groqKey, includeOllama, ollamaBaseUrl, DEFAULT_GEMINI_BASE_URL);
    }

    public DefaultProviderClientFactory(String openAiKey, String claudeKey, String geminiKey,
                                        String groqKey, boolean includeOllama, String ollamaBaseUrl,
                                        String geminiBaseUrl) {
        this.openAiKey = openAiKey;
        this.claudeKey = claudeKey;
        this.geminiKey = geminiKey;
        this.groqKey = groqKey;
        this.includeOllama = includeOllama;
        this.ollamaBaseUrl = ollamaBaseUrl;
        this.geminiBaseUrl = geminiBaseUrl;
    }

    @Override
    public List<ProviderClient> createClients() {
        List<ProviderClient> clients = new ArrayList<>();
        if (hasText(openAiKey)) clients.add(new OpenAiClient(openAiKey));
        if (hasText(claudeKey)) clients.add(new ClaudeClient(claudeKey));
        if (hasText(geminiKey)) clients.add(new GeminiClient(geminiKey, geminiBaseUrl));
        if (hasText(groqKey)) clients.add(new GroqClient(groqKey));
        if (includeOllama) clients.add(new OllamaClient(ollamaBaseUrl));
        return clients;
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}