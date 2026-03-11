package com.unillm.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Maps the 'unillm.providers' section of application.yml
 * into a strongly-typed Java config object.
 */
@Configuration
@ConfigurationProperties(prefix = "unillm.providers")
public class ProviderProperties {

    private ProviderConfig openai = new ProviderConfig();
    private ProviderConfig gemini = new ProviderConfig();

    public ProviderConfig getOpenai() { return openai; }
    public void setOpenai(ProviderConfig openai) { this.openai = openai; }
    public ProviderConfig getGemini() { return gemini; }
    public void setGemini(ProviderConfig gemini) { this.gemini = gemini; }

    /**
     * Configuration for a single provider.
     */
    public static class ProviderConfig {
        private String apiKey;
        private String baseUrl;
        private String defaultModel;

        public String getApiKey() { return apiKey; }
        public void setApiKey(String apiKey) { this.apiKey = apiKey; }
        public String getBaseUrl() { return baseUrl; }
        public void setBaseUrl(String baseUrl) { this.baseUrl = baseUrl; }
        public String getDefaultModel() { return defaultModel; }
        public void setDefaultModel(String defaultModel) { this.defaultModel = defaultModel; }
    }
}
