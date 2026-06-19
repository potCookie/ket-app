package com.ketapp.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.ketapp.entity.Task;
import com.ketapp.mapper.TaskMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Auto-generates daily KET learning content by scraping web materials
 * and combining them with curriculum templates.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ContentGeneratorService {

    private final TaskMapper taskMapper;
    private final AudioService audioService;
    private final LlmContentGenerator llmGenerator;
    private final ObjectMapper objectMapper = new ObjectMapper();

    private static final Random RANDOM = ThreadLocalRandom.current();

    /**
     * Generate content for a specific day number (1-35) on a given date.
     * Returns true if generation was successful.
     */
    public boolean generateDay(LocalDate date, int dayNumber) {
        int weekIdx = KetCurriculum.getWeekIndex(dayNumber);
        int dayIdx = KetCurriculum.getDayInWeekIndex(dayNumber);

        // Check if already exists
        if (taskMapper.selectCount(
                new LambdaQueryWrapper<Task>().eq(Task::getTaskDate, date)) > 0) {
            log.info("Task for date {} already exists, skipping.", date);
            return false;
        }

        String[] weekTheme = KetCurriculum.getWeekTheme(weekIdx);
        String[] dailyTopic = KetCurriculum.getDailyTopic(weekIdx, dayIdx);
        String[][] vocab = KetCurriculum.getVocabulary(weekIdx, dayIdx);
        String[] grammar = KetCurriculum.getGrammar(weekIdx, dayIdx);

        String theme = weekTheme[1] + " · " + dailyTopic[1];

        try {
            // Attempt AI-powered generation first
            ObjectNode aiResult = llmGenerator.generateDailyContent(
                    weekIdx + 1, dayNumber,
                    dailyTopic[0], dailyTopic[1],
                    weekTheme, grammar[0], grammar[1],
                    vocab);

            // 1. Vocabulary section (always from curriculum)
            ObjectNode vocabData = generateVocabSection(vocab);

            // 2-7. Try AI sections, fall back to template
            ObjectNode readingData;
            ObjectNode listeningData;
            ObjectNode grammarData;
            ObjectNode speakingData;
            ObjectNode writingData;
            String parentNote;

            if (aiResult != null) {
                log.info("Using AI-generated content for day {}", dayNumber);
                readingData = extractOrFallback(aiResult, "reading",
                        () -> generateReadingSection(dailyTopic[0], dayNumber));
                listeningData = extractOrFallback(aiResult, "listening",
                        () -> generateListeningSection(readingData, dayNumber));
                grammarData = extractAiGrammar(aiResult, grammar, vocab);
                speakingData = extractOrFallback(aiResult, "speaking",
                        () -> generateSpeakingSection(vocab, weekTheme, dailyTopic, weekIdx, dayIdx));
                writingData = extractOrFallback(aiResult, "writing",
                        () -> generateWritingSection(dailyTopic, readingData, weekIdx, dayIdx));
                parentNote = extractParentNote(aiResult, weekIdx, dayIdx, weekTheme, dailyTopic, grammar);
            } else {
                log.info("Using template-based generation for day {}", dayNumber);
                readingData = generateReadingSection(dailyTopic[0], dayNumber);
                listeningData = generateListeningSection(readingData, dayNumber);
                grammarData = generateGrammarSection(grammar, vocab, dayNumber);
                speakingData = generateSpeakingSection(vocab, weekTheme, dailyTopic, weekIdx, dayIdx);
                writingData = generateWritingSection(dailyTopic, readingData, weekIdx, dayIdx);
                parentNote = generateParentNote(weekIdx, dayIdx, weekTheme, dailyTopic, grammar);
            }

            // Build Task entity
            Task task = new Task();
            task.setTaskDate(date);
            task.setWeek(weekIdx + 1);
            task.setDay(dayNumber);
            task.setWeekday(KetCurriculum.getChineseWeekday(date));
            task.setTheme(theme);
            task.setDuration("35分钟");
            task.setVocabData(objectMapper.writeValueAsString(vocabData));
            task.setReadingData(objectMapper.writeValueAsString(readingData));
            task.setListeningData(objectMapper.writeValueAsString(listeningData));
            task.setGrammarData(objectMapper.writeValueAsString(grammarData));
            task.setSpeakingData(objectMapper.writeValueAsString(speakingData));
            task.setWritingData(objectMapper.writeValueAsString(writingData));
            task.setParentNote(parentNote);

            taskMapper.insert(task);
            log.info("Generated task for date: {}, theme: {}", date, theme);

            // Generate audio for listening questions
            if (listeningData.has("questions")) {
                for (JsonNode q : listeningData.get("questions")) {
                    if (q.has("audio_text")) {
                        try {
                            audioService.generateAudio(q.get("audio_text").asText());
                        } catch (Exception e) {
                            log.warn("Audio generation failed: {}", e.getMessage());
                        }
                    }
                }
            }

            return true;
        } catch (Exception e) {
            log.error("Failed to generate content for day {}: {}", dayNumber, e.getMessage(), e);
            return false;
        }
    }

    // ==================== Section Generators ====================

    private ObjectNode generateVocabSection(String[][] vocab) {
        ObjectNode node = objectMapper.createObjectNode();
        node.put("group", vocab[0][1] + "相关"); // e.g. "名字相关"
        ArrayNode words = objectMapper.createArrayNode();
        for (String[] w : vocab) {
            ObjectNode word = objectMapper.createObjectNode();
            word.put("en", w[0]);
            word.put("zh", w[1]);
            word.put("eg", w[2]);
            words.add(word);
        }
        node.set("words", words);
        return node;
    }

    private ObjectNode generateReadingSection(String topicEn, int dayNumber) {
        ObjectNode node = objectMapper.createObjectNode();

        // Try to scrape a reading passage
        String[] passageData = scrapeReadingPassage(topicEn, dayNumber);

        ObjectNode passage = objectMapper.createObjectNode();
        passage.put("title", passageData[0]);
        passage.put("text", passageData[1]);
        passage.put("translation", passageData[2]);
        node.set("passage", passage);

        // Generate 3 comprehension questions from the passage
        ArrayNode questions = objectMapper.createArrayNode();
        String[][] questionData = generateReadingQuestions(passageData[1]);
        for (String[] qd : questionData) {
            ObjectNode q = objectMapper.createObjectNode();
            q.put("q", qd[0]);
            ArrayNode opts = objectMapper.createArrayNode();
            for (String o : qd[1].split("\\|")) opts.add(o);
            q.set("options", opts);
            q.put("answer", qd[2]);
            questions.add(q);
        }
        node.set("questions", questions);

        return node;
    }

    private ObjectNode generateListeningSection(ObjectNode readingData, int dayNumber) {
        ObjectNode node = objectMapper.createObjectNode();
        ArrayNode questions = objectMapper.createArrayNode();

        // Create 5 short listening scenarios based on the reading theme
        String passageText = readingData.get("passage").get("text").asText();
        String[] sentences = passageText.split("(?<=[.!?])\\s+");

        for (int i = 0; i < Math.min(5, Math.max(3, sentences.length)); i++) {
            String sentence = sentences[i].trim();
            if (sentence.length() < 10) continue;

            String[] qd = generateListeningQuestion(sentence, i + 1);
            ObjectNode q = objectMapper.createObjectNode();
            q.put("id", i + 1);
            q.put("scenario", qd[0]);      // question
            q.put("translation", qd[1]);    // Chinese translation
            q.put("audio_text", sentence);  // the sentence to speak
            ArrayNode opts = objectMapper.createArrayNode();
            for (String o : qd[2].split("\\|")) opts.add(o);
            q.set("options", opts);
            q.put("answer", qd[3]);
            q.put("key_word", qd[4]);
            questions.add(q);
        }

        node.set("questions", questions);
        return node;
    }

    private ObjectNode generateGrammarSection(String[] grammar, String[][] vocab, int dayNumber) {
        ObjectNode node = objectMapper.createObjectNode();
        node.put("point", grammar[0]);
        node.put("explanation", grammar[1]);

        // Generate 5 fill-in-the-blank exercises using vocab words
        ArrayNode exercises = objectMapper.createArrayNode();
        ArrayNode answers = objectMapper.createArrayNode();
        ArrayNode analysis = objectMapper.createArrayNode();

        String[][] samples = generateGrammarExercises(grammar[0], vocab, dayNumber);
        for (String[] sample : samples) {
            exercises.add(sample[0]); // sentence with blank
            answers.add(sample[1]);   // answer
            analysis.add(sample[2]);  // explanation
        }

        node.set("exercises", exercises);
        node.set("answers", answers);
        node.set("analysis", analysis);
        return node;
    }

    private ObjectNode generateSpeakingSection(String[][] vocab, String[] weekTheme,
                                                String[] dailyTopic, int weekIdx, int dayIdx) {
        ObjectNode node = objectMapper.createObjectNode();

        // Build a template with blanks using 6 of the vocab words
        String[][] selectedVocab = pickRandomVocab(vocab, 6);
        String template = buildSpeakingTemplate(selectedVocab, dailyTopic, weekIdx, dayIdx);
        node.put("template", template);

        // Key phrases
        ArrayNode phrases = objectMapper.createArrayNode();
        for (int i = 0; i < Math.min(5, selectedVocab.length); i++) {
            ObjectNode p = objectMapper.createObjectNode();
            String[] v = selectedVocab[i];
            p.put("en", "I " + v[0] + " ...");
            p.put("zh", "我" + v[1] + "……");
            phrases.add(p);
        }
        node.set("phrases", phrases);

        return node;
    }

    private ObjectNode generateWritingSection(String[] dailyTopic, ObjectNode readingData,
                                               int weekIdx, int dayIdx) {
        ObjectNode node = objectMapper.createObjectNode();

        String topicEn = dailyTopic[0];
        String prompt = String.format("Write 3-5 sentences about %s. Try to use the words you learned today.", topicEn);
        node.put("prompt", prompt);

        // Use a short version of reading passage as sample
        String passage = readingData.get("passage").get("text").asText();
        String[] sentences = passage.split("(?<=[.!?])\\s+");
        StringBuilder sample = new StringBuilder();
        for (int i = 0; i < Math.min(5, sentences.length); i++) {
            if (i > 0) sample.append(" ");
            sample.append(sentences[i].trim());
        }
        node.put("sample", sample.toString());

        return node;
    }

    private String generateParentNote(int weekIdx, int dayIdx, String[] weekTheme,
                                       String[] dailyTopic, String[] grammar) {
        return String.format(
            "今天是第%d周第%d天，主题：%s - %s。\n" +
            "核心任务：①学习10个%s相关词汇；②掌握语法点：%s；③阅读短文并做理解题；④听力训练。\n" +
            "建议：先让孩子玩词汇翻卡游戏（5分钟），再按顺序完成各模块。家长可以陪读短文，帮孩子理解意思。",
            weekIdx + 1, dayIdx + 1,
            weekTheme[1], dailyTopic[1],
            dailyTopic[1], grammar[0]);
    }

    // ==================== Web Scraping ====================

    private String[] scrapeReadingPassage(String topic, int dayNumber) {
        // Try multiple sources
        String[] result = tryScrape(topic);
        if (result != null) return result;

        // Fallback: generate from template
        return generateFallbackPassage(topic, dayNumber);
    }

    private String[] tryScrape(String topic) {
        try {
            // Search breakingnewsenglish.com for graded readers
            String searchUrl = "https://breakingnewsenglish.com/search.html?q=" +
                    java.net.URLEncoder.encode(topic, "UTF-8");
            Document doc = Jsoup.connect(searchUrl)
                    .userAgent("Mozilla/5.0")
                    .timeout(10000)
                    .get();

            Elements links = doc.select("a[href*='.html']");
            for (Element link : links) {
                String href = link.absUrl("href");
                if (href.contains("breakingnewsenglish.com") && !href.contains("search")) {
                    // Found a potential article, try to get content
                    String[] article = scrapeArticle(href);
                    if (article != null) return article;
                }
            }
        } catch (Exception e) {
            log.debug("Scraping failed for topic '{}': {}", topic, e.getMessage());
        }
        return null;
    }

    private String[] scrapeArticle(String url) {
        try {
            Document doc = Jsoup.connect(url)
                    .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                    .timeout(8000)
                    .get();

            String title = doc.title();
            Elements paragraphs = doc.select("article p, .article p, #article p");

            if (paragraphs.isEmpty()) {
                // Try generic paragraph selection
                paragraphs = doc.select("p");
            }

            StringBuilder text = new StringBuilder();
            int count = 0;
            for (Element p : paragraphs) {
                String pt = p.text().trim();
                if (pt.length() > 20 && pt.length() < 500) {
                    if (count > 0) text.append(" ");
                    text.append(pt);
                    count++;
                    if (text.length() > 800) break;
                }
            }

            if (text.length() < 100) return null;

            // Simple translation note (real translation would need an API)
            String translation = "（请参考中文翻译理解短文大意）";

            return new String[]{
                title.replaceAll("\\s*-\\s*Breaking News English.*", "").trim(),
                text.toString().trim(),
                translation
            };
        } catch (Exception e) {
            return null;
        }
    }

    // ==================== Fallback Content Generation ====================

    private String[] generateFallbackPassage(String topic, int dayNumber) {
        // Build a passage from vocabulary and topic
        String[] passages = {
            // Sample passage template
            "Hello! Today we are learning about " + topic.toLowerCase() + ". "
            + "Many children enjoy learning new things every day. "
            + "It is important to practice English at home and at school. "
            + "Reading, writing, listening and speaking are all important skills. "
            + "Let's read this passage together and answer some questions. "
            + "Learning English can be fun when you practice every day. "
            + "Don't forget to ask your parents or teachers for help when you need it.",
        };

        String text = passages[RANDOM.nextInt(passages.length)];
        String translation = "你好！今天我们学习关于" + topic + "的内容。"
            + "很多孩子喜欢每天学习新东西。"
            + "在家和在学校练习英语很重要。"
            + "阅读、写作、听力和口语都是重要的技能。"
            + "让我们一起读这段短文并回答问题。"
            + "每天练习可以让学习英语变得有趣。"
            + "当你需要帮助时，别忘了问你的父母或老师。";

        return new String[]{
            "Learning About " + topic,
            text,
            translation
        };
    }

    // ==================== Question Generators ====================

    private String[][] generateReadingQuestions(String passage) {
        String[] sentences = passage.split("(?<=[.!?])\\s+");
        List<String[]> questions = new ArrayList<>();

        for (int i = 0; i < sentences.length && questions.size() < 3; i++) {
            String sentence = sentences[i].trim();
            if (sentence.length() < 20) continue;

            // Generate a WH-question and multiple choice options
            String[] q = generateQuestionFromSentence(sentence);
            if (q != null) questions.add(q);
        }

        // If not enough, add generic questions
        while (questions.size() < 3) {
            questions.add(new String[]{
                "What is the main topic of this passage?",
                "A. Sports|B. " + sentences[0].split(" ")[0] + "|C. Music",
                "B"
            });
        }

        return questions.toArray(new String[0][]);
    }

    private String[] generateQuestionFromSentence(String sentence) {
        String[] words = sentence.split("\\s+");
        if (words.length < 5) return null;

        // Generate a simple WH-question based on sentence structure
        String lower = sentence.toLowerCase();

        if (lower.contains(" is ") || lower.contains(" are ")) {
            // "X is Y" -> "What is X?"
            int isIdx = lower.indexOf(" is ");
            if (isIdx == -1) isIdx = lower.indexOf(" are ");
            String subject = sentence.substring(0, isIdx).trim();
            String predicate = sentence.substring(isIdx + (lower.contains(" is ") ? 4 : 5)).trim();

            // Remove trailing period
            predicate = predicate.replaceAll("[.!?]$", "");

            // Create wrong options
            String wrong1 = predicate.length() > 3 ? predicate.substring(0, 1).toUpperCase() + "wrong1" : "Something else";
            String wrong2 = "Not mentioned";

            return new String[]{
                "What " + (lower.contains(" are ") ? "are" : "is") + " " + subject + "?",
                "A. " + predicate.substring(0, Math.min(predicate.length(), 20)) + "|B. " + wrong1 + "|C. " + wrong2,
                "A"
            };
        }

        if (lower.contains(" like ") || lower.contains(" likes ")) {
            int likeIdx = lower.contains(" likes ") ? lower.indexOf(" likes ") : lower.indexOf(" like ");
            String subject = sentence.substring(0, likeIdx).trim();
            return new String[]{
                "What does " + subject + " like?",
                "A. " + words[words.length-1].replaceAll("[.!?]", "") + "|B. Sports|C. Music",
                "A"
            };
        }

        // Generic: ask about the sentence
        String keyWord = words[Math.min(words.length - 1, RANDOM.nextInt(words.length) + 2)].replaceAll("[.!?,]", "");
        return new String[]{
            "What does the passage say about " + keyWord + "?",
            "A. " + keyWord + " is important|B. " + keyWord + " is not mentioned|C. We should avoid " + keyWord,
            "A"
        };
    }

    private String[] generateListeningQuestion(String sentence, int id) {
        String lower = sentence.toLowerCase();

        // Question scenario based on sentence meaning
        String scenario, translation, correctAnswer, wrong1, wrong2, keyWord;

        if (lower.contains(" time ") || lower.contains(" o'clock")) {
            scenario = "What time is mentioned?";
            translation = "提到了什么时间？";
            correctAnswer = "A. " + extractTime(sentence);
            wrong1 = "B. 8:00";
            wrong2 = "C. 9:30";
            keyWord = extractTime(sentence);
        } else if (lower.contains(" like ") || lower.contains(" likes ") || lower.contains(" love ")) {
            scenario = "What does the person like?";
            translation = "这个人喜欢什么？";
            String thing = extractLikeObject(sentence);
            correctAnswer = "A. " + thing;
            wrong1 = "B. " + (thing.length() > 2 ? thing.charAt(0) + "xxx" : "Dancing");
            wrong2 = "C. Reading";
            keyWord = thing;
        } else if (lower.contains(" color ") || lower.contains(" colour ")) {
            scenario = "What colour is mentioned?";
            translation = "提到了什么颜色？";
            correctAnswer = "A. Red";
            wrong1 = "B. Blue";
            wrong2 = "C. Green";
            keyWord = "red";
        } else {
            // Generic listener question
            String[] words = sentence.split("\\s+");
            keyWord = words[Math.min(words.length-1, 3)].replaceAll("[.!?,]", "");
            scenario = "What does the speaker say about " + keyWord + "?";
            translation = "说话者关于" + keyWord + "说了什么？";
            correctAnswer = "A. " + sentence.substring(0, Math.min(30, sentence.length()));
            wrong1 = "B. Something different";
            wrong2 = "C. Not mentioned";
        }

        return new String[]{scenario, translation, correctAnswer + "|" + wrong1 + "|" + wrong2, "A", keyWord};
    }

    private String[][] generateGrammarExercises(String grammarPoint, String[][] vocab, int dayNumber) {
        List<String[]> exercises = new ArrayList<>();

        if (grammarPoint.contains("am/is/are") || grammarPoint.contains("'to be'")) {
            String[] subjects = {"I", "She", "They", "Tom and I", "My mother"};
            String[] answersArr = {"am", "is", "are", "are", "is"};
            String[] expl = {"I 搭配 am", "第三人称单数用 is", "复数用 are", "Tom and I = we，用 are", "My mother = she，用 is"};
            for (int i = 0; i < 5; i++) {
                exercises.add(new String[]{
                    subjects[i] + " ___ a student.", answersArr[i], expl[i]
                });
            }
        } else if (grammarPoint.contains("Present Simple")) {
            String[] sents = {"I ___ up at 7 o'clock.", "She ___ to school every day.", "We ___ breakfast together.", "He ___ TV in the evening.", "They ___ in the park."};
            String[] ans = {"get", "goes", "eat", "watches", "play"};
            String[] exp = {"I / you / we / they 用原形", "he / she / it 加 -s/-es", "we 用原形", "he 加 -es", "they 用原形"};
            for (int i = 0; i < 5; i++) {
                exercises.add(new String[]{sents[i], ans[i], exp[i]});
            }
        } else if (grammarPoint.contains("Can")) {
            String[] sents = {"I ___ swim very well.", "She ___ play the piano.", "___ you help me?", "He ___ run fast.", "They ___ speak English."};
            String[] ans = {"can", "can", "Can", "can", "can"};
            String[] exp = {"can + 动词原形", "can + 动词原形", "疑问句 Can 提前", "can + 动词原形", "can + 动词原形"};
            for (int i = 0; i < 5; i++) {
                exercises.add(new String[]{sents[i], ans[i], exp[i]});
            }
        } else if (grammarPoint.contains("Past Simple")) {
            String[] sents = {"Yesterday I ___ (go) to the park.", "She ___ (eat) an apple.", "They ___ (play) football.", "He ___ (watch) TV.", "We ___ (visit) grandma."};
            String[] ans = {"went", "ate", "played", "watched", "visited"};
            String[] exp = {"go 的过去式是 went", "eat 的过去式是 ate", "规则动词加 -ed", "规则动词加 -ed", "规则动词加 -ed"};
            for (int i = 0; i < 5; i++) {
                exercises.add(new String[]{sents[i], ans[i], exp[i]});
            }
        } else {
            // Generic exercises using vocab
            for (int i = 0; i < 5 && i < vocab.length; i++) {
                exercises.add(new String[]{
                    "I like " + vocab[i][0] + ".", vocab[i][0], "用正确的单词填空"
                });
            }
        }

        return exercises.toArray(new String[0][]);
    }

    // ==================== Helpers ====================

    private String[][] pickRandomVocab(String[][] vocab, int count) {
        List<String[]> list = new ArrayList<>(Arrays.asList(vocab));
        Collections.shuffle(list, RANDOM);
        return list.subList(0, Math.min(count, list.size())).toArray(new String[0][0]);
    }

    private String buildSpeakingTemplate(String[][] words, String[] dailyTopic, int weekIdx, int dayIdx) {
        StringBuilder sb = new StringBuilder();
        sb.append("Hello! My name is _____. I am _____ years old. ");
        sb.append("Today I want to talk about ").append(dailyTopic[0].toLowerCase()).append(". ");
        for (int i = 0; i < words.length; i++) {
            if (i == 0) sb.append("I like ").append(words[i][0]).append(". ");
            else if (i == 1) sb.append("I also like ").append(words[i][0]).append(". ");
            else if (i == 2) sb.append("My ").append(words[i][0]).append(" is _____. ");
            else if (i == 3) sb.append("I can ").append(words[i][0]).append(". ");
            else if (i == 4) sb.append("I have a ").append(words[i][0]).append(". ");
            else sb.append("I enjoy ").append(words[i][0]).append(".");
        }
        return sb.toString().trim();
    }

    private String extractTime(String sentence) {
        java.util.regex.Matcher m = java.util.regex.Pattern.compile("\\d{1,2}(:\\d{2})?\\s*(o'clock|am|pm)?").matcher(sentence);
        return m.find() ? m.group() : "7:00";
    }

    private String extractLikeObject(String sentence) {
        String lower = sentence.toLowerCase();
        int idx = lower.indexOf(" likes ");
        if (idx == -1) idx = lower.indexOf(" like ");
        if (idx == -1) return "something";
        String after = sentence.substring(idx + (lower.contains(" likes ") ? 7 : 6));
        return after.split("[.,]")[0].trim();
    }

    // ==================== AI Content Extractors ====================

    /**
     * Extract a section from AI result, fall back to template generator if missing.
     */
    private ObjectNode extractOrFallback(ObjectNode ai, String section,
                                          java.util.function.Supplier<ObjectNode> fallback) {
        if (ai.has(section) && !ai.get(section).isNull()) {
            return (ObjectNode) ai.get(section);
        }
        return fallback.get();
    }

    /**
     * Extract grammar from AI result, converting to our expected format.
     */
    private ObjectNode extractAiGrammar(ObjectNode ai, String[] grammar, String[][] vocab) {
        if (!ai.has("grammar") || ai.get("grammar").isNull()) {
            return generateGrammarSection(grammar, vocab, 0);
        }
        JsonNode aiGrammar = ai.get("grammar");
        ObjectNode result = objectMapper.createObjectNode();
        result.put("point", grammar[0]);
        result.put("explanation", grammar[1]);

        ArrayNode exercises = objectMapper.createArrayNode();
        ArrayNode answers = objectMapper.createArrayNode();
        ArrayNode analysis = objectMapper.createArrayNode();

        if (aiGrammar.has("exercises") && aiGrammar.get("exercises").isArray()) {
            for (JsonNode ex : aiGrammar.get("exercises")) {
                String sentence = ex.has("sentence") ? ex.get("sentence").asText() : ex.asText();
                String answer = ex.has("answer") ? ex.get("answer").asText() : "";
                exercises.add(sentence);
                answers.add(answer);
            }
        }
        if (aiGrammar.has("analysis") && aiGrammar.get("analysis").isArray()) {
            for (JsonNode a : aiGrammar.get("analysis")) {
                analysis.add(a.asText());
            }
        }

        // Pad to 5 if needed
        while (exercises.size() < 5) {
            exercises.add("Fill in the blank.");
            answers.add("");
            analysis.add("Review the grammar point above.");
        }

        result.set("exercises", exercises);
        result.set("answers", answers);
        result.set("analysis", analysis);
        return result;
    }

    private String extractParentNote(ObjectNode ai, int weekIdx, int dayIdx,
                                      String[] weekTheme, String[] dailyTopic, String[] grammar) {
        if (ai.has("parent_note") && !ai.get("parent_note").isNull()) {
            return ai.get("parent_note").asText();
        }
        return generateParentNote(weekIdx, dayIdx, weekTheme, dailyTopic, grammar);
    }

    /**
     * Generate all 35 days of content starting from a base date.
     */
    public int generateAll(LocalDate startDate) {
        int count = 0;
        for (int day = 1; day <= 35; day++) {
            LocalDate date = startDate.plusDays(day - 1);
            if (generateDay(date, day)) {
                count++;
            }
        }
        return count;
    }

    /**
     * Ensure content exists for today. If not, generate it.
     */
    public boolean ensureToday(LocalDate today, int dayNumber) {
        if (dayNumber < 1 || dayNumber > 35) {
            log.info("Day {} is outside the 35-day plan.", dayNumber);
            return false;
        }
        return generateDay(today, dayNumber);
    }
}
