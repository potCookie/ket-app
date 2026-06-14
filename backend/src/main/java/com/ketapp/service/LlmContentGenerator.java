package com.ketapp.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Uses LLM to generate high-quality KET learning content.
 * Builds structured prompts and parses JSON responses.
 * Falls back gracefully if LLM is unavailable.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LlmContentGenerator {

    private final LlmService llmService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    private static final String SYSTEM_PROMPT = """
        You are a native English-speaking Cambridge KET (A2 Key) English exam expert and children's English teacher.
        Your job is to create engaging, age-appropriate English learning content for Chinese children aged 7-12.
        
        CRITICAL QUALITY RULES:
        - ALL English MUST be grammatically perfect with ZERO spelling mistakes
        - Use natural, idiomatic English that a native English child would use
        - NO Chinglish, NO machine-translated phrasing, NO awkward expressions
        - Reading passages must read like authentic English children's books
        - All English must be at CEFR A1-A2 level (KET level)
        - Sentences should be short and simple (8-15 words max)
        - Vocabulary must be from the KET word list
        - Content must be fun and relatable for children
        - Correct answer must genuinely be the right answer to the question
        - Plausible wrong options (not random words, not "Not mentioned")
        - Always output valid JSON matching the requested format exactly
        - Do NOT include markdown code fences in your response, just raw JSON
        - Chinese translations must be natural and child-friendly
        """;

    /**
     * Generate complete daily learning content via LLM.
     * Returns a JSON node with all sections, or null on failure.
     */
    public ObjectNode generateDailyContent(
            int week, int day, String topicEn, String topicZh,
            String[] weekTheme, String grammarPoint, String grammarExplain,
            String[][] vocabWords) {

        String vocabJson = buildVocabList(vocabWords);

        String userPrompt = String.format("""
            Generate a complete Day %d (Week %d) KET learning lesson.
            
            Week theme: %s / %s
            Today's topic: %s (%s)
            Grammar point: %s - %s
            
            Today's vocabulary (10 words):
            %s
            
            Generate ALL of the following sections in this JSON structure:
            {
              "reading": {
                "passage": {
                  "title": "catchy title",
                  "text": "passage of 80-150 words using today's vocabulary, A1-A2 level",
                  "translation": "natural Chinese translation"
                },
                "questions": [
                  {"q": "question?", "options": ["A. ...", "B. ...", "C. ..."], "answer": "A/B/C"}
                  // exactly 3 questions
                ]
              },
              "listening": {
                "questions": [
                  {
                    "scenario": "question about what you hear (in English)",
                    "translation": "Chinese translation",
                    "audio_text": "one short sentence to be spoken (8-15 words)",
                    "options": ["A. ...", "B. ...", "C. ..."],
                    "answer": "A/B/C",
                    "key_word": "key word from audio_text"
                  }
                  // exactly 5 questions, each with different audio_text
                ]
              },
              "grammar": {
                "exercises": [
                  {"sentence": "I ___ a student.", "answer": "am"},
                  {"sentence": "She ___ my teacher.", "answer": "is"},
                  ...5 total...
                ],
                "analysis": [
                  "explanation for exercise 1",
                  "explanation for exercise 2",
                  ...5 total...
                ]
              },
              "speaking": {
                "template": "fill-in-blank template with ____ for blanks, 4-6 sentences",
                "phrases": [
                  {"en": "key phrase", "zh": "Chinese meaning"},
                  ...4-5 total...
                ]
              },
              "writing": {
                "prompt": "writing task description for children",
                "sample": "2-3 sentence model answer"
              },
              "parent_note": "brief note for parents in Chinese (2-3 sentences)"
            }
            
            Output ONLY the JSON object, no other text.
            """,
            day, week,
            weekTheme[0], weekTheme[1],
            topicEn, topicZh,
            grammarPoint, grammarExplain,
            vocabJson
        );

        log.info("Requesting LLM content for Week {} Day {}: {}", week, day, topicEn);

        String response = llmService.chat(SYSTEM_PROMPT, userPrompt);
        if (response == null) {
            return null;
        }

        ObjectNode result = parseResponse(response);
        if (result == null) {
            log.warn("Failed to parse LLM response for Week {} Day {}", week, day);
        }
        return result;
    }

    /**
     * Parse LLM response, handling common formatting issues.
     */
    private ObjectNode parseResponse(String text) {
        // Try to extract JSON from response (handle markdown code fences)
        String json = extractJson(text);
        if (json == null) return null;

        try {
            return (ObjectNode) objectMapper.readTree(json);
        } catch (JsonProcessingException e) {
            log.error("JSON parse error: {}", e.getMessage());
            return null;
        }
    }

    private String extractJson(String text) {
        // Remove markdown code fences: ```json ... ```
        Pattern p = Pattern.compile("```(?:json)?\\s*([\\s\\S]*?)```");
        Matcher m = p.matcher(text);
        if (m.find()) {
            return m.group(1).trim();
        }

        // Try to find { ... } block
        int start = text.indexOf('{');
        int end = text.lastIndexOf('}');
        if (start >= 0 && end > start) {
            return text.substring(start, end + 1).trim();
        }

        return null;
    }

    private String buildVocabList(String[][] words) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < words.length; i++) {
            sb.append(String.format("  %d. %s - %s (e.g. %s)\n", i + 1, words[i][0], words[i][1], words[i][2]));
        }
        return sb.toString();
    }
}
