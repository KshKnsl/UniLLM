package unillm.api;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URLDecoder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import unillm.ChatMessage;
import unillm.ChatRequest;
import unillm.ChatResponse;
import unillm.UniLLM;
import unillm.core.HttpJson;
import unillm.providers.DefaultProviderClientFactory;

public final class UniLLMApiServer {
    private static final String DEFAULT_OLLAMA_URL = "http://localhost:11434";
    private static final String HEADER_OPENAI_KEY = "x-unillm-openai-key";
    private static final String HEADER_ANTHROPIC_KEY = "x-unillm-anthropic-key";
    private static final String HEADER_GEMINI_KEY = "x-unillm-gemini-key";
    private static final String HEADER_GROQ_KEY = "x-unillm-groq-key";
    private static final String HEADER_INCLUDE_OLLAMA = "x-unillm-include-ollama";
    private static final String HEADER_OLLAMA_URL = "x-unillm-ollama-base-url";
    private static final Path WEB_ROOT = Paths.get("frontend", "dist");

    private final HttpServer server;

    public UniLLMApiServer(int port) throws IOException {
        this.server = HttpServer.create(new InetSocketAddress(port), 0);
        this.server.createContext("/api/health", new HealthHandler());
        this.server.createContext("/api/providers", new ProvidersHandler());
        this.server.createContext("/api/models", new ModelsHandler());
        this.server.createContext("/api/chat", new ChatHandler());
        this.server.createContext("/", new StaticHandler());
    }

    public void start() {
        server.start();
        System.out.println("UniLLM API server running on http://localhost:" + server.getAddress().getPort());
    }

    public static void main(String[] args) throws Exception {
        int port = parsePort(args);
        new UniLLMApiServer(port).start();
    }

    private static UniLLM createLlm(Map<String, String> config) {
        String openAiKey = firstNonBlank(config.get(HEADER_OPENAI_KEY), System.getenv("OPENAI_API_KEY"));
        String anthropicKey = firstNonBlank(config.get(HEADER_ANTHROPIC_KEY), System.getenv("ANTHROPIC_API_KEY"));
        String geminiKey = firstNonBlank(config.get(HEADER_GEMINI_KEY), System.getenv("GEMINI_API_KEY"));
        String groqKey = firstNonBlank(config.get(HEADER_GROQ_KEY), System.getenv("GROQ_API_KEY"));
        boolean includeOllama = parseBoolean(firstNonBlank(config.get(HEADER_INCLUDE_OLLAMA), System.getenv("UNILLM_INCLUDE_OLLAMA")));
        String ollamaBaseUrl = firstNonBlank(config.get(HEADER_OLLAMA_URL), System.getenv("UNILLM_OLLAMA_BASE_URL"), DEFAULT_OLLAMA_URL);

        return UniLLM.fromFactory(new DefaultProviderClientFactory(
                openAiKey,
                anthropicKey,
                geminiKey,
                groqKey,
                includeOllama,
                ollamaBaseUrl
        ));
    }

    private static boolean parseBoolean(String value) {
        return value != null && Boolean.parseBoolean(value.trim());
    }

    private static String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value.trim();
            }
        }
        return null;
    }

    private UniLLM resolveLlm(HttpExchange exchange) {
        Map<String, String> config = new LinkedHashMap<>();
        Headers headers = exchange.getRequestHeaders();
        copyHeader(config, headers, HEADER_OPENAI_KEY);
        copyHeader(config, headers, HEADER_ANTHROPIC_KEY);
        copyHeader(config, headers, HEADER_GEMINI_KEY);
        copyHeader(config, headers, HEADER_GROQ_KEY);
        copyHeader(config, headers, HEADER_INCLUDE_OLLAMA);
        copyHeader(config, headers, HEADER_OLLAMA_URL);
        return createLlm(config);
    }

    private void copyHeader(Map<String, String> out, Headers headers, String name) {
        List<String> values = headers.get(name);
        if (values != null && !values.isEmpty()) {
            out.put(name, values.get(0));
        }
    }

    private static UniLLM createDefaultLlm() {
        Map<String, String> env = System.getenv();
        boolean includeOllama = Boolean.parseBoolean(env.getOrDefault("UNILLM_INCLUDE_OLLAMA", "false"));
        String ollamaBaseUrl = env.getOrDefault("UNILLM_OLLAMA_BASE_URL", DEFAULT_OLLAMA_URL);

        DefaultProviderClientFactory factory = new DefaultProviderClientFactory(
                env.get("OPENAI_API_KEY"),
                env.get("ANTHROPIC_API_KEY"),
                env.get("GEMINI_API_KEY"),
                env.get("GROQ_API_KEY"),
                includeOllama
        );

        if (includeOllama && !DEFAULT_OLLAMA_URL.equals(ollamaBaseUrl)) {
            List<unillm.ProviderClient> clients = new ArrayList<>();
            for (unillm.ProviderClient client : factory.createClients()) {
                if ("ollama".equals(client.name())) {
                    clients.add(new unillm.providers.OllamaClient(ollamaBaseUrl));
                } else {
                    clients.add(client);
                }
            }
            return new UniLLM(clients);
        }

        return UniLLM.fromFactory(factory);
    }

    private static int parsePort(String[] args) {
        String explicit = System.getenv("PORT");
        if (explicit == null || explicit.isBlank()) {
            explicit = System.getenv("UNILLM_PORT");
        }
        if (args != null && args.length > 0) {
            for (String arg : args) {
                if (arg.startsWith("--port=")) {
                    explicit = arg.substring("--port=".length());
                }
            }
        }
        if (explicit == null || explicit.isBlank()) {
            return 8080;
        }
        return Integer.parseInt(explicit.trim());
    }

    private abstract class BaseHandler implements HttpHandler {
        @Override
        public final void handle(HttpExchange exchange) throws IOException {
            addCorsHeaders(exchange.getResponseHeaders());
            if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
                exchange.sendResponseHeaders(204, -1);
                exchange.close();
                return;
            }

            try {
                handleRequest(exchange);
            } catch (IllegalArgumentException e) {
                sendJson(exchange, 400, error("bad_request", e.getMessage()));
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                sendJson(exchange, 500, error("interrupted", "Request interrupted"));
            } catch (Exception e) {
                sendJson(exchange, 500, error("server_error", e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage()));
            } finally {
                exchange.close();
            }
        }

        protected abstract void handleRequest(HttpExchange exchange) throws Exception;
    }

    private final class HealthHandler extends BaseHandler {
        @Override
        protected void handleRequest(HttpExchange exchange) throws IOException {
            requireMethod(exchange, "GET");
            ObjectNode body = HttpJson.MAPPER.createObjectNode();
            body.put("ok", true);
            body.put("service", "unillm");
            sendJson(exchange, 200, body);
        }
    }

    private final class ProvidersHandler extends BaseHandler {
        @Override
        protected void handleRequest(HttpExchange exchange) throws IOException {
            requireMethod(exchange, "GET");
            UniLLM llm = resolveLlm(exchange);
            ArrayNode providers = HttpJson.MAPPER.createArrayNode();
            for (String provider : llm.providers()) {
                providers.add(provider);
            }
            ObjectNode body = HttpJson.MAPPER.createObjectNode();
            body.set("providers", providers);
            sendJson(exchange, 200, body);
        }
    }

    private final class ModelsHandler extends BaseHandler {
        @Override
        protected void handleRequest(HttpExchange exchange) throws IOException, InterruptedException {
            requireMethod(exchange, "GET");
            UniLLM llm = resolveLlm(exchange);
            Map<String, String> query = parseQuery(exchange.getRequestURI());
            String provider = query.get("provider");

            if (provider != null && !provider.isBlank()) {
                ObjectNode body = HttpJson.MAPPER.createObjectNode();
                body.put("provider", provider);
                ArrayNode models = HttpJson.MAPPER.createArrayNode();
                for (String model : llm.listModels(provider)) {
                    models.add(model);
                }
                body.set("models", models);
                sendJson(exchange, 200, body);
                return;
            }

            Map<String, List<String>> allModels = llm.listAllModels();
            ObjectNode body = HttpJson.MAPPER.createObjectNode();
            ObjectNode providers = body.putObject("providers");
            for (Map.Entry<String, List<String>> entry : allModels.entrySet()) {
                ArrayNode models = providers.putArray(entry.getKey());
                for (String model : entry.getValue()) {
                    models.add(model);
                }
            }
            sendJson(exchange, 200, body);
        }
    }

    private final class ChatHandler extends BaseHandler {
        @Override
        protected void handleRequest(HttpExchange exchange) throws IOException, InterruptedException {
            requireMethod(exchange, "POST");
            UniLLM llm = resolveLlm(exchange);
            JsonNode body = readJsonBody(exchange);
            if (body == null || body.isNull()) {
                throw new IllegalArgumentException("Request body is required");
            }

            String model = requiredText(body, "model");
            String prompt = optionalText(body, "prompt");
            String system = optionalText(body, "system");
            Double temperature = optionalDouble(body, "temperature");
            Integer maxTokens = optionalInt(body, "maxTokens");

            List<ChatMessage> messages = parseMessages(body);
            if (messages.isEmpty()) {
                if (prompt == null || prompt.isBlank()) {
                    throw new IllegalArgumentException("Either prompt or messages must be provided");
                }
                messages.add(ChatMessage.user(prompt));
            }

            ChatRequest request = new ChatRequest(model, messages, system, temperature, maxTokens);
            ChatResponse response = llm.chat(request);

            ObjectNode out = HttpJson.MAPPER.createObjectNode();
            out.put("provider", response.provider());
            out.put("model", response.model());
            out.put("text", response.text());
            sendJson(exchange, 200, out);
        }
    }

    private final class StaticHandler extends BaseHandler {
        @Override
        protected void handleRequest(HttpExchange exchange) throws IOException {
            String method = exchange.getRequestMethod();
            if (!"GET".equalsIgnoreCase(method) && !"HEAD".equalsIgnoreCase(method)) {
                throw new IllegalArgumentException("Unsupported method: " + method);
            }

            Path indexFile = WEB_ROOT.resolve("index.html");
            if (!Files.exists(indexFile)) {
                sendPlainText(exchange, 404, "Frontend build not found");
                return;
            }

            String requestPath = exchange.getRequestURI().getPath();
            Path resolved = resolveStaticPath(requestPath);
            if (resolved == null) {
                sendPlainText(exchange, 404, "Not found");
                return;
            }

            Path file = Files.exists(resolved) && !Files.isDirectory(resolved) ? resolved : indexFile;
            byte[] bytes = Files.readAllBytes(file);
            String contentType = Files.probeContentType(file);
            if (contentType == null) {
                contentType = file.getFileName().toString().endsWith(".html")
                        ? "text/html; charset=utf-8"
                        : "application/octet-stream";
            }

            sendBytes(exchange, 200, bytes, contentType);
        }
    }

    private void requireMethod(HttpExchange exchange, String method) {
        if (!method.equalsIgnoreCase(exchange.getRequestMethod())) {
            throw new IllegalArgumentException("Unsupported method: " + exchange.getRequestMethod());
        }
    }

    private JsonNode readJsonBody(HttpExchange exchange) throws IOException {
        try (InputStream input = exchange.getRequestBody(); ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            byte[] buffer = new byte[4096];
            int read;
            while ((read = input.read(buffer)) != -1) {
                output.write(buffer, 0, read);
            }
            String raw = output.toString(StandardCharsets.UTF_8);
            if (raw.isBlank()) {
                return null;
            }
            return HttpJson.MAPPER.readTree(raw);
        }
    }

    private List<ChatMessage> parseMessages(JsonNode body) {
        List<ChatMessage> messages = new ArrayList<>();
        JsonNode rawMessages = body.path("messages");
        if (!rawMessages.isArray()) {
            return messages;
        }

        for (JsonNode item : rawMessages) {
            String role = optionalText(item, "role");
            String content = optionalText(item, "content");
            if (role == null || role.isBlank() || content == null) {
                continue;
            }
            messages.add(new ChatMessage(role, content));
        }
        return messages;
    }

    private String requiredText(JsonNode body, String field) {
        String value = optionalText(body, field);
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " is required");
        }
        return value;
    }

    private String optionalText(JsonNode body, String field) {
        JsonNode value = body.get(field);
        if (value == null || value.isNull()) {
            return null;
        }
        String text = value.asText();
        return text == null || text.isBlank() ? null : text;
    }

    private Double optionalDouble(JsonNode body, String field) {
        JsonNode value = body.get(field);
        return value == null || value.isNull() ? null : value.asDouble();
    }

    private Integer optionalInt(JsonNode body, String field) {
        JsonNode value = body.get(field);
        return value == null || value.isNull() ? null : value.asInt();
    }

    private Map<String, String> parseQuery(URI uri) {
        Map<String, String> query = new LinkedHashMap<>();
        String rawQuery = uri.getRawQuery();
        if (rawQuery == null || rawQuery.isBlank()) {
            return query;
        }

        for (String pair : rawQuery.split("&")) {
            String[] parts = pair.split("=", 2);
            String key = decode(parts[0]);
            String value = parts.length > 1 ? decode(parts[1]) : "";
            query.put(key, value);
        }
        return query;
    }

    private String decode(String value) {
        return URLDecoder.decode(value, StandardCharsets.UTF_8);
    }

    private void addCorsHeaders(Headers headers) {
        headers.set("Access-Control-Allow-Origin", "*");
        headers.set("Access-Control-Allow-Methods", "GET,POST,OPTIONS");
        headers.set("Access-Control-Allow-Headers", "Content-Type,x-unillm-openai-key,x-unillm-anthropic-key,x-unillm-gemini-key,x-unillm-groq-key,x-unillm-include-ollama,x-unillm-ollama-base-url");
        headers.set("Content-Type", "application/json; charset=utf-8");
    }

    private Path resolveStaticPath(String requestPath) throws IOException {
        if (requestPath == null || requestPath.isBlank() || "/".equals(requestPath)) {
            return WEB_ROOT.resolve("index.html");
        }

        String normalizedPath = requestPath.startsWith("/") ? requestPath.substring(1) : requestPath;
        Path resolved = WEB_ROOT.resolve(normalizedPath).normalize();
        if (!resolved.startsWith(WEB_ROOT.normalize())) {
            return null;
        }

        if (Files.exists(resolved) && !Files.isDirectory(resolved)) {
            return resolved;
        }

        int lastSlash = normalizedPath.lastIndexOf('/');
        int lastDot = normalizedPath.lastIndexOf('.');
        if (lastDot > lastSlash) {
            return null;
        }

        return WEB_ROOT.resolve("index.html");
    }

    private void sendBytes(HttpExchange exchange, int statusCode, byte[] bytes, String contentType) throws IOException {
        exchange.getResponseHeaders().set("Content-Type", contentType);
        boolean headRequest = "HEAD".equalsIgnoreCase(exchange.getRequestMethod());
        exchange.sendResponseHeaders(statusCode, headRequest ? -1 : bytes.length);
        if (!headRequest) {
            exchange.getResponseBody().write(bytes);
            exchange.getResponseBody().flush();
        }
    }

    private void sendPlainText(HttpExchange exchange, int statusCode, String message) throws IOException {
        byte[] bytes = message.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "text/plain; charset=utf-8");
        boolean headRequest = "HEAD".equalsIgnoreCase(exchange.getRequestMethod());
        exchange.sendResponseHeaders(statusCode, headRequest ? -1 : bytes.length);
        if (!headRequest) {
            exchange.getResponseBody().write(bytes);
            exchange.getResponseBody().flush();
        }
    }

    private void sendJson(HttpExchange exchange, int statusCode, JsonNode body) throws IOException {
        byte[] bytes = HttpJson.MAPPER.writeValueAsBytes(body);
        boolean headRequest = "HEAD".equalsIgnoreCase(exchange.getRequestMethod());
        exchange.sendResponseHeaders(statusCode, headRequest ? -1 : bytes.length);
        if (!headRequest) {
            exchange.getResponseBody().write(bytes);
            exchange.getResponseBody().flush();
        }
    }

    private ObjectNode error(String code, String message) {
        ObjectNode body = HttpJson.MAPPER.createObjectNode();
        body.put("error", code);
        body.put("message", message == null ? "" : message);
        return body;
    }
}