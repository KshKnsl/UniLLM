package com.unillm.model;

/**
 * A single message in a conversation (role + content).
 * Roles: "system", "user", "assistant"
 */
public record AiMessage(
        String role,
        String content
) {
    public static AiMessage user(String content) {
        return new AiMessage("user", content);
    }

    public static AiMessage assistant(String content) {
        return new AiMessage("assistant", content);
    }

    public static AiMessage system(String content) {
        return new AiMessage("system", content);
    }
}
