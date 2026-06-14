package com.ketapp.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ketapp.entity.Task;
import com.ketapp.mapper.TaskMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.Iterator;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class DataImportService {

    private final TaskMapper taskMapper;
    private final AudioService audioService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public void importIfEmpty() {
        if (taskMapper.selectCount(new LambdaQueryWrapper<>()) > 0) {
            log.info("Task data already exists, skipping import.");
            return;
        }

        try {
            ClassPathResource resource = new ClassPathResource("KET_daily_plan.json");
            JsonNode root = objectMapper.readTree(resource.getInputStream());
            JsonNode days = root.get("days");

            Iterator<Map.Entry<String, JsonNode>> fields = days.fields();
            while (fields.hasNext()) {
                Map.Entry<String, JsonNode> entry = fields.next();
                String dateStr = entry.getKey();
                JsonNode dayData = entry.getValue();

                Task task = new Task();
                task.setTaskDate(LocalDate.parse(dateStr));
                task.setWeek(dayData.get("week").asInt());
                task.setDay(dayData.get("day").asInt());
                task.setWeekday(dayData.has("weekday") ? dayData.get("weekday").asText() : "");
                task.setTheme(dayData.get("theme").asText());
                task.setDuration(dayData.has("duration") ? dayData.get("duration").asText() : "35分钟");

                if (dayData.has("vocab")) {
                    task.setVocabData(objectMapper.writeValueAsString(dayData.get("vocab")));
                }
                if (dayData.has("grammar")) {
                    task.setGrammarData(objectMapper.writeValueAsString(dayData.get("grammar")));
                }
                if (dayData.has("reading")) {
                    task.setReadingData(objectMapper.writeValueAsString(dayData.get("reading")));
                }
                if (dayData.has("listening")) {
                    task.setListeningData(objectMapper.writeValueAsString(dayData.get("listening")));
                }
                if (dayData.has("speaking")) {
                    task.setSpeakingData(objectMapper.writeValueAsString(dayData.get("speaking")));
                }
                if (dayData.has("writing")) {
                    task.setWritingData(objectMapper.writeValueAsString(dayData.get("writing")));
                }
                if (dayData.has("parent_note")) {
                    task.setParentNote(dayData.get("parent_note").asText());
                }

                taskMapper.insert(task);
                log.info("Imported task for date: {}", dateStr);

                // Pre-generate audio for listening questions
                if (dayData.has("listening")) {
                    JsonNode listening = dayData.get("listening");
                    if (listening.has("questions")) {
                        for (JsonNode q : listening.get("questions")) {
                            if (q.has("audio_text")) {
                                String audioText = q.get("audio_text").asText();
                                try {
                                    String filename = audioService.generateAudio(audioText);
                                    log.info("  Audio generated: {} -> {}", audioText, filename);
                                } catch (Exception e) {
                                    log.warn("  Failed to generate audio for '{}': {}", audioText, e.getMessage());
                                }
                            }
                        }
                    }
                }
            }

            log.info("Data import completed successfully.");
        } catch (Exception e) {
            log.error("Failed to import data: {}", e.getMessage(), e);
        }
    }
}
