package unillm;

import java.io.IOException;
import java.util.List;

public interface ProviderClient {
    String name();
    boolean supports(String model);
    ChatResponse chat(ChatRequest request) throws IOException, InterruptedException;
    List<String> listModels() throws IOException, InterruptedException;
}
