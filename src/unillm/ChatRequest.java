package unillm;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class ChatRequest {
    private final String model;
    private final List<ChatMessage> messages;
    private final String system;
    private final Double temperature;
    private final Integer maxTokens;

    public ChatRequest(String model, List<ChatMessage> messages, String system, Double temperature, Integer maxTokens) {
        this.model = model;
        this.messages = Collections.unmodifiableList(new ArrayList<ChatMessage>(messages));
        this.system = system;
        this.temperature = temperature;
        this.maxTokens = maxTokens;
    }

    public String model() {
        return model;
    }

    public List<ChatMessage> messages() {
        return messages;
    }

    public String system() {
        return system;
    }

    public Double temperature() {
        return temperature;
    }

    public Integer maxTokens() {
        return maxTokens;
    }

    public static ChatRequest of(String model, String prompt) {
        return new ChatRequest(model, List.of(ChatMessage.user(prompt)), null, null, null);
    }
}
