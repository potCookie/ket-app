package com.ketapp.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "ket.llm")
public class LlmConfig {
    /** Whether LLM generation is enabled */
    private boolean enabled = false;
    /** OpenAI-compatible API endpoint */
    private String apiUrl = "https://api.openai.com/v1/chat/completions";
    /** API key (supports env var KET_LLM_API_KEY) */
    private String apiKey = "";
    /** Model name */
    private String model = "gpt-4o-mini";
    /** Max tokens per request */
    private int maxTokens = 4096;
    /** Generation temperature */
    private double temperature = 0.7;

    public boolean isConfigured() {
        return enabled && apiKey != null && !apiKey.isBlank();
    }
}
