# UniLLM

Minimal Java package for calling multiple model providers with one interface.

## Supported Providers

- OpenAI: models starting with `gpt`
- Claude: models starting with `claude`
- Gemini: models starting with `gemini`
- Ollama: models in form `ollama/<model>`

## Requirements

- Java 11+

## Build JAR

javac -d out src/unillm/_.java src/unillm/core/_.java src/unillm/providers/\*.java
javac -cp out Demo.java  
java -cp "out;." Demo

## Quick Library Usage

```java
import unillm.ChatResponse;
import unillm.UniLLM;

UniLLM llm = UniLLM.defaultClients(
    System.getenv("OPENAI_API_KEY"),
    System.getenv("ANTHROPIC_API_KEY"),
    System.getenv("GEMINI_API_KEY")
);

ChatResponse a = llm.chat("gpt-4o-mini", "Say hi");
ChatResponse b = llm.chat("claude-3-5-sonnet-latest", "List 3 git tips");
ChatResponse c = llm.chat("gemini-2.0-flash", "Write one-line summary of Java streams");
ChatResponse d = llm.chat("ollama/llama3.1", "Explain polymorphism in one paragraph");

System.out.println(a.text());
```