package com.ketapp.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "ket.llm")
public class LlmConfig {
    /** 是否启用LLM生成 */
    private boolean enabled = false;
    /** OpenAI兼容的API端点 */
    private String apiUrl = "https://api.openai.com/v1/chat/completions";
    /** API密钥（支持环境变量KET_LLM_API_KEY） */
    private String apiKey = "";
    /** 模型名称 */
    private String model = "gpt-4o-mini";
    /** 每次请求的最大token数 */
    private int maxTokens = 4096;
    /** 生成温度 */
    private double temperature = 0.7;

    public boolean isConfigured() {
        return enabled && apiKey != null && !apiKey.isBlank();
    }
}
