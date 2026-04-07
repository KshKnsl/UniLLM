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

Linux/macOS:

```bash
mkdir -p out dist && javac -encoding UTF-8 -d out $(find src -name "*.java") && jar --create --file dist/unillm.jar -C out .
```

Windows (PowerShell):

```powershell
New-Item -ItemType Directory -Path out, dist -Force | Out-Null; $sources = Get-ChildItem -Path src -Filter "*.java" -Recurse | ForEach-Object { $_.FullName }; javac -encoding UTF-8 -d out $sources; & "C:\Program Files\Java\jdk-24\bin\jar.exe" --create --file dist/unillm.jar -C out .
```

This creates `dist/unillm.jar` with all compiled classes.

## Run Demo

Demo source is in `Demo.java`.

Windows (PowerShell):

```powershell
$sources = Get-ChildItem -Path src -Filter "*.java" -Recurse | ForEach-Object { $_.FullName }; javac -encoding UTF-8 -d out $sources; javac -cp out -d out Demo.java; java -cp out Demo
```

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
