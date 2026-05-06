import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import unillm.ProviderClientFactory;
import unillm.UniLLM;
import unillm.providers.DefaultProviderClientFactory;

public class Demo {
    public static void main(String[] args) throws Exception {
        Map<String, String> keys = loadApiKeys(".env");
        
        ProviderClientFactory factory = new DefaultProviderClientFactory(
                keys.get("OPENAI_API_KEY"),
                keys.get("ANTHROPIC_API_KEY"),
                keys.get("GEMINI_API_KEY"),
                keys.get("GROQ_API_KEY"),
                false
        );
        UniLLM llm = UniLLM.fromFactory(factory);

        Map<String, List<String>> allModels = llm.listAllModels();
        for (Map.Entry<String, List<String>> entry : allModels.entrySet()) {
            System.out.println("=== " + entry.getKey() + " ===");
            for (String model : entry.getValue()) {
                System.out.println(model);
            }
            System.out.println();
        }
    }
    
    private static Map<String, String> loadApiKeys(String filePath) throws IOException {
        Map<String, String> keys = new HashMap<>();
        try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty() || line.startsWith("#")) {
                    continue;
                }
                String[] parts = line.split("=", 2);
                if (parts.length == 2) {
                    keys.put(parts[0].trim(), parts[1].trim());
                }
            }
        }
        return keys;
    }
}
