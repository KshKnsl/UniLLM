package demo;

import unillm.ChatResponse;
import unillm.UniLLM;

public class Demo {
    public static void main(String[] args) throws Exception {
        UniLLM llm = UniLLM.defaultClients(
                System.getenv("OPENAI_API_KEY"),
                System.getenv("ANTHROPIC_API_KEY"),
                System.getenv("GEMINI_API_KEY")
        );

        ChatResponse response = llm.chat("ollama/llama3.1", "Say hello in one sentence.");
        System.out.println(response.provider() + ": " + response.text());
    }
}
