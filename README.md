# UniLLM

Minimal Java package for calling multiple model providers with one interface.
The library keeps orchestration in `UniLLM` and moves provider construction into a factory so the core stays open for extension.

This repo now also includes a Java API server and a React frontend in `frontend/`.

## Supported Providers

- OpenAI: models starting with `gpt`
- Claude: models starting with `claude`
- Gemini: models starting with `gemini`
- Groq: models in form `groq/<model>`
- Ollama: models in form `ollama/<model>`

## Requirements

- Java 11+

## Build And Run

Java API:

```bash
javac -cp "lib/*" -d out src/unillm/*.java src/unillm/core/*.java src/unillm/providers/*.java src/unillm/api/*.java Demo.java
java -cp "lib/*;out;." unillm.api.UniLLMApiServer
```

React frontend:

```bash
cd frontend
npm install
npm run dev
```

The Vite dev server proxies `/api` requests to `http://localhost:8080`.

## Quick Library Usage

```java
import unillm.ChatResponse;
import unillm.ProviderClientFactory;
import unillm.UniLLM;
import unillm.providers.DefaultProviderClientFactory;

ProviderClientFactory factory = new DefaultProviderClientFactory(
    System.getenv("OPENAI_API_KEY"),
    System.getenv("ANTHROPIC_API_KEY"),
    System.getenv("GEMINI_API_KEY"),
    System.getenv("GROQ_API_KEY"),
    true
);

UniLLM llm = UniLLM.fromFactory(factory);

ChatResponse a = llm.chat("gpt-4o-mini", "Say hi");
ChatResponse b = llm.chat("claude-3-5-sonnet-latest", "List 3 git tips");
ChatResponse c = llm.chat("gemini-2.0-flash", "Write one-line summary of Java streams");
ChatResponse d = llm.chat("ollama/llama3.1", "Explain polymorphism in one paragraph");

System.out.println(a.text());
```