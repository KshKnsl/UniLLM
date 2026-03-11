package com.unillm.service;

import com.unillm.client.AiClient;
import com.unillm.model.AiRequest;
import com.unillm.model.AiResponse;
import com.unillm.model.AiStreamChunk;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * The brain of the gateway!
 *
 * This service uses the Factory pattern to route incoming requests
 * to the correct AI provider based on the model name.
 *
 * All registered AiClient beans are auto-injected by Spring.
 */
@Service
public class AiGatewayService {

    private static final Logger log = LoggerFactory.getLogger(AiGatewayService.class);

    private final List<AiClient> clients;
    private final Map<String, AiClient> providerMap;

    /**
     * Spring injects ALL beans implementing AiClient automatically.
     */
    public AiGatewayService(List<AiClient> clients) {
        this.clients = clients;
        this.providerMap = clients.stream()
                .collect(Collectors.toMap(AiClient::getProvider, c -> c));
        log.info("🚀 UniLLM Gateway initialized with {} providers: {}",
                clients.size(),
                clients.stream().map(AiClient::getProvider).toList());
    }

    /**
     * Route a request to the appropriate provider and return a full response.
     */
    public Mono<AiResponse> generate(AiRequest request) {
        AiClient client = resolveClient(request.model());
        log.info("Routing '{}' → {}", request.model(), client.getProvider());
        return client.generate(request);
    }

    /**
     * Route a request and return a streaming response (Flux of chunks).
     */
    public Flux<AiStreamChunk> generateStream(AiRequest request) {
        AiClient client = resolveClient(request.model());
        log.info("Streaming '{}' → {}", request.model(), client.getProvider());
        return client.generateStream(request);
    }

    /**
     * List all registered providers.
     */
    public List<String> listProviders() {
        return clients.stream().map(AiClient::getProvider).toList();
    }

    /**
     * Resolve the correct AiClient for a given model name.
     * Uses the supports() method from each client.
     */
    private AiClient resolveClient(String model) {
        if (model == null || model.isBlank()) {
            throw new IllegalArgumentException(
                    "Model name is required. Available providers: " + listProviders());
        }

        return clients.stream()
                .filter(c -> c.supports(model))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(
                        "No provider found for model: '" + model +
                                "'. Available providers: " + listProviders()));
    }
}
