package com.unillm.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.List;

/**
 * Unified AI response returned by the gateway.
 * Wraps the provider-specific response into a standard format.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record AiResponse(
        String id,
        String provider,
        String model,
        List<AiMessage> choices,
        Usage usage,
        long createdAt
) {
    public record Usage(
            int promptTokens,
            int completionTokens,
            int totalTokens
    ) {}

    /**
     * Quick builder for a single-choice response.
     */
    public static AiResponse of(String provider, String model, String content) {
        return new AiResponse(
                java.util.UUID.randomUUID().toString(),
                provider,
                model,
                List.of(AiMessage.assistant(content)),
                null,
                System.currentTimeMillis()
        );
    }
}
