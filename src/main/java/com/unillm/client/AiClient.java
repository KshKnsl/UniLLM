package com.unillm.client;

import com.unillm.model.AiRequest;
import com.unillm.model.AiResponse;
import com.unillm.model.AiStreamChunk;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Core contract for any AI provider.
 * Each provider (OpenAI, Gemini, etc.) implements this interface.
 *
 * This is the Strategy pattern — the gateway picks the right
 * strategy at runtime based on the requested model.
 */
public interface AiClient {

    /**
     * @return The provider name (e.g., "openai", "gemini").
     */
    String getProvider();

    /**
     * @return true if this client can handle the given model name.
     */
    boolean supports(String model);

    /**
     * Generate a complete (non-streaming) response.
     */
    Mono<AiResponse> generate(AiRequest request);

    /**
     * Generate a streaming response (token by token via SSE).
     */
    Flux<AiStreamChunk> generateStream(AiRequest request);
}
