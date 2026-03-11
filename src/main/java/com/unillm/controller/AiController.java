package com.unillm.controller;

import com.unillm.model.AiRequest;
import com.unillm.model.AiResponse;
import com.unillm.model.AiStreamChunk;
import com.unillm.service.AiGatewayService;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;

/**
 * REST controller exposing the unified AI Gateway endpoints.
 *
 * Endpoints:
 *   POST /api/chat/completions      → full (non-streaming) response
 *   POST /api/chat/stream           → SSE streaming response
 *   GET  /api/providers             → list available providers
 *   GET  /api/health                → health check
 */
@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "*") // Allow frontend dev servers
public class AiController {

    private final AiGatewayService gatewayService;

    public AiController(AiGatewayService gatewayService) {
        this.gatewayService = gatewayService;
    }

    /**
     * Standard chat completion — returns the full response at once.
     *
     * Example request body:
     * {
     *   "model": "gpt-3.5-turbo",
     *   "messages": [{"role": "user", "content": "Hello!"}],
     *   "temperature": 0.7
     * }
     */
    @PostMapping("/chat/completions")
    public Mono<AiResponse> chatCompletion(@RequestBody AiRequest request) {
        return gatewayService.generate(request);
    }

    /**
     * Streaming chat completion — returns tokens as Server-Sent Events.
     * The frontend can consume this with EventSource or fetch + ReadableStream.
     *
     * Each event is an AiStreamChunk JSON with a "delta" field containing
     * the next piece of generated text.
     */
    @PostMapping(value = "/chat/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<AiStreamChunk> chatStream(@RequestBody AiRequest request) {
        return gatewayService.generateStream(request);
    }

    /**
     * List all registered AI providers.
     */
    @GetMapping("/providers")
    public Mono<Map<String, List<String>>> listProviders() {
        return Mono.just(Map.of("providers", gatewayService.listProviders()));
    }

    /**
     * Health check endpoint.
     */
    @GetMapping("/health")
    public Mono<Map<String, String>> health() {
        return Mono.just(Map.of(
                "status", "UP",
                "service", "UniLLM Gateway",
                "version", "1.0.0"
        ));
    }
}
