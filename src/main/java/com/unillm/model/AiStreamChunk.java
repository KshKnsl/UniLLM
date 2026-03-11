package com.unillm.model;

/**
 * Represents a single streamed chunk of text from a provider.
 * Used for Server-Sent Events (SSE) streaming endpoints.
 */
public record AiStreamChunk(
        String provider,
        String model,
        String delta,
        boolean finished
) {
    public static AiStreamChunk text(String provider, String model, String delta) {
        return new AiStreamChunk(provider, model, delta, false);
    }

    public static AiStreamChunk done(String provider, String model) {
        return new AiStreamChunk(provider, model, "", true);
    }
}
