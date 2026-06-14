package com.ketapp.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.ketapp.config.LlmConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

/**
 * OpenAI-compatible LLM API service.
 * Supports OpenAI, DeepSeek, Moonshot, and any OpenAI-format API.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LlmService {

    private final LlmConfig llmConfig;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final RestTemplate restTemplate = new RestTemplate();

    /**
     * Call the LLM with a system prompt and user message.
     * Returns the assistant's response text, or null on failure.
     */
    public String chat(String systemPrompt, String userMessage) {
        if (!llmConfig.isConfigured()) {
            log.warn("LLM not configured, skipping API call.");
            return null;
        }

        try {
            ObjectNode body = objectMapper.createObjectNode();
            body.put("model", llmConfig.getModel());
            body.put("temperature", llmConfig.getTemperature());
            body.put("max_tokens", llmConfig.getMaxTokens());

            ArrayNode messages = objectMapper.createArrayNode();
            ObjectNode sysMsg = objectMapper.createObjectNode();
            sysMsg.put("role", "system");
            sysMsg.put("content", systemPrompt);
            messages.add(sysMsg);

            ObjectNode userMsg = objectMapper.createObjectNode();
            userMsg.put("role", "user");
            userMsg.put("content", userMessage);
            messages.add(userMsg);

            body.set("messages", messages);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(llmConfig.getApiKey());

            HttpEntity<String> request = new HttpEntity<>(objectMapper.writeValueAsString(body), headers);

            log.info("Calling LLM: {} with model {}", llmConfig.getApiUrl(), llmConfig.getModel());
            ResponseEntity<String> response = restTemplate.postForEntity(
                    llmConfig.getApiUrl(), request, String.class);

            if (response.getBody() == null) {
                log.error("LLM returned empty body");
                return null;
            }

            JsonNode root = objectMapper.readTree(response.getBody());
            JsonNode choices = root.get("choices");
            if (choices == null || !choices.isArray() || choices.isEmpty()) {
                log.error("LLM response has no choices: {}", response.getBody().substring(0, Math.min(200, response.getBody().length())));
                return null;
            }

            JsonNode content = choices.get(0).get("message").get("content");
            if (content == null) {
                log.error("LLM response has no content");
                return null;
            }

            String text = content.asText().trim();
            log.info("LLM response received: {} chars", text.length());
            return text;
        } catch (Exception e) {
            log.error("LLM API call failed: {}", e.getMessage());
            return null;
        }
    }
}
