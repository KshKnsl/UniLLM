package com.unillm.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.List;

/**
 * Unified AI request that works across all providers.
 * The gateway translates this into provider-specific formats.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record AiRequest(
        String model,
        List<AiMessage> messages,
        String systemInstruction,
        Double temperature,
        Integer maxTokens,
        Boolean stream
) {
    /**
     * Convenience constructor for a simple single-prompt request.
     */
    public static AiRequest of(String model, String prompt) {
        return new AiRequest(
                model,
                List.of(new AiMessage("user", prompt)),
                null, null, null, false
        );
    }
}
