# UniLLM Gateway 🚀

A **unified AI Gateway** built in Java with Spring Boot. One API to rule them all — route requests to **OpenAI**, **Google Gemini**, and more, through a single standardized interface.

## ✨ Features

- 🔀 **Unified API** — One endpoint, multiple AI providers
- 🌊 **Streaming (SSE)** — Real-time token-by-token responses via Server-Sent Events
- 🏗️ **Strategy Pattern** — Clean OOP design; add new providers by implementing one interface
- 🛡️ **Error Handling** — Global exception handler with clean JSON error responses
- ⚡ **Reactive** — Built on Spring WebFlux for non-blocking, high-performance I/O

## 📂 Project Structure

```
src/main/java/com/unillm/
├── UniLlmApplication.java          # Spring Boot entry point
├── model/
│   ├── AiRequest.java              # Unified request format
│   ├── AiMessage.java              # Chat message (role + content)
│   ├── AiResponse.java             # Unified response format
│   └── AiStreamChunk.java          # Single streamed token chunk
├── client/
│   ├── AiClient.java               # Provider interface (Strategy)
│   ├── OpenAiClient.java           # OpenAI implementation
│   └── GeminiClient.java           # Google Gemini implementation
├── service/
│   └── AiGatewayService.java       # Route requests → correct provider
├── controller/
│   ├── AiController.java           # REST endpoints
│   └── GlobalExceptionHandler.java # Error handling
└── config/
    ├── ProviderProperties.java      # YAML config binding
    └── WebClientConfig.java         # HTTP client setup
```

## 🚀 Getting Started

### Prerequisites
- Java 17+
- Maven 3.8+
- API keys for at least one provider (OpenAI or Gemini)

### 1. Set API Keys

Set environment variables:
```bash
export OPENAI_API_KEY=sk-your-key-here
export GEMINI_API_KEY=your-gemini-key-here
```

Or edit `src/main/resources/application.yml` directly.

### 2. Build & Run

```bash
mvn clean install
mvn spring-boot:run
```

The server starts on **http://localhost:8080**.

## 📡 API Endpoints

### Health Check
```bash
GET /api/health
```

### List Providers
```bash
GET /api/providers
```

### Chat Completion (Non-Streaming)
```bash
POST /api/chat/completions
Content-Type: application/json

{
  "model": "gpt-3.5-turbo",
  "messages": [
    {"role": "user", "content": "Explain Java records in 3 sentences."}
  ],
  "temperature": 0.7
}
```

### Chat Completion (Streaming / SSE)
```bash
POST /api/chat/stream
Content-Type: application/json

{
  "model": "gemini-2.0-flash",
  "messages": [
    {"role": "user", "content": "Write a short poem about coding."}
  ]
}
```

## 🔌 Supported Models

| Provider | Model Prefix | Examples |
|----------|-------------|----------|
| OpenAI   | `gpt`       | `gpt-3.5-turbo`, `gpt-4`, `gpt-4o` |
| Gemini   | `gemini`    | `gemini-pro`, `gemini-2.0-flash`, `gemini-1.5-pro` |

## 🏗️ Adding a New Provider

1. Create a class implementing `AiClient` in `com.unillm.client`
2. Annotate with `@Component`
3. Implement `getProvider()`, `supports()`, `generate()`, and `generateStream()`
4. Add config in `application.yml` and `ProviderProperties.java`

Spring auto-discovers it — **zero changes needed** in the gateway service or controller!

## 🎓 Design Patterns Used

- **Strategy Pattern** — `AiClient` interface with swappable provider implementations
- **Factory Pattern** — `AiGatewayService` resolves the right client at runtime
- **Dependency Injection** — Spring auto-wires all `AiClient` beans
- **Record Classes** — Java records for immutable DTOs
- **Reactive Streams** — Project Reactor (`Mono`/`Flux`) for async, non-blocking I/O
