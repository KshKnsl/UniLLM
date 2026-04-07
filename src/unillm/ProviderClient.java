package unillm;

import java.io.IOException;

public interface ProviderClient {
    String name();
    boolean supports(String model);
    ChatResponse chat(ChatRequest request) throws IOException, InterruptedException;
}
