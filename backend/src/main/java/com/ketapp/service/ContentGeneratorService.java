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
 * 通过网络抓取材料并结合课程模板自动生成每日KET学习内容
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
     * 为指定日期生成特定天数（1-35）的内容
     * 如果生成成功则返回true
     */
    public boolean generateDay(LocalDate date, int dayNumber) {
        int weekIdx = KetCurriculum.getWeekIndex(dayNumber);
        int dayIdx = KetCurriculum.getDayInWeekIndex(dayNumber);

        // 检查是否已存在
        if (taskMapper.selectCount(
                new LambdaQueryWrapper<Task>().eq(Task::getTaskDate, date)) > 0) {
            log.info("日期 {} 的任务已存在，跳过。", date);
            return false;
        }

        String[] weekTheme = KetCurriculum.getWeekTheme(weekIdx);
        String[] dailyTopic = KetCurriculum.getDailyTopic(weekIdx, dayIdx);
        String[][] vocab = KetCurriculum.getVocabulary(weekIdx, dayIdx);
        String[] grammar = KetCurriculum.getGrammar(weekIdx, dayIdx);

        String theme = weekTheme[1] + " · " + dailyTopic[1];

        try {
            // 首先尝试AI生成
            ObjectNode aiResult = llmGenerator.generateDailyContent(
                    weekIdx + 1, dayNumber,
                    dailyTopic[0], dailyTopic[1],
                    weekTheme, grammar[0], grammar[1],
                    vocab);

            // 1. 词汇部分（始终从课程中获取）
            ObjectNode vocabData = generateVocabSection(vocab);

            // 2-7. 尝试AI部分，失败则回退到模板
            ObjectNode readingData;
            ObjectNode listeningData;
            ObjectNode grammarData;
            ObjectNode speakingData;
            ObjectNode writingData;
            String parentNote;

            if (aiResult != null) {
                log.info("使用AI生成第{}天的内容", dayNumber);
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
                log.info("使用模板生成第{}天的内容", dayNumber);
                readingData = generateReadingSection(dailyTopic[0], dayNumber);
                listeningData = generateListeningSection(readingData, dayNumber);
                grammarData = generateGrammarSection(grammar, vocab, dayNumber);
                speakingData = generateSpeakingSection(vocab, weekTheme, dailyTopic, weekIdx, dayIdx);
                writingData = generateWritingSection(dailyTopic, readingData, weekIdx, dayIdx);
                parentNote = generateParentNote(weekIdx, dayIdx, weekTheme, dailyTopic, grammar);
            }

            // 构建Task实体
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
            log.info("为日期 {} 生成任务，主题: {}", date, theme);

            // 为听力问题生成音频
            if (listeningData.has("questions")) {
                for (JsonNode q : listeningData.get("questions")) {
                    if (q.has("audio_text")) {
                        try {
                            audioService.generateAudio(q.get("audio_text").asText());
                        } catch (Exception e) {
                            log.warn("音频生成失败: {}", e.getMessage());
                        }
                    }
                }
            }

            return true;
        } catch (Exception e) {
            log.error("生成第{}天内容失败: {}", dayNumber, e.getMessage(), e);
            return false;
        }
    }

    // ==================== 各模块生成器 ====================

    private ObjectNode generateVocabSection(String[][] vocab) {
        ObjectNode node = objectMapper.createObjectNode();
        node.put("group", vocab[0][1] + "相关"); // 例如："名字相关"
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

        // 尝试抓取阅读材料
        String[] passageData = scrapeReadingPassage(topicEn, dayNumber);

        ObjectNode passage = objectMapper.createObjectNode();
        passage.put("title", passageData[0]);
        passage.put("text", passageData[1]);
        passage.put("translation", passageData[2]);
        node.set("passage", passage);

        // 从材料中生成3个理解题
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

        // 创建5个基于阅读主题的简短听力场景
        String passageText = readingData.get("passage").get("text").asText();
        String[] sentences = passageText.split("(?<=[.!?])\\s+");

        for (int i = 0; i < Math.min(5, Math.max(3, sentences.length)); i++) {
            String sentence = sentences[i].trim();
            if (sentence.length() < 10) continue;

            String[] qd = generateListeningQuestion(sentence, i + 1);
            ObjectNode q = objectMapper.createObjectNode();
            q.put("id", i + 1);
            q.put("scenario", qd[0]);      // 问题
            q.put("translation", qd[1]);    // 中文翻译
            q.put("audio_text", sentence);  // 要朗读的句子
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

        // 生成5个填空练习（使用词汇单词）
        ArrayNode exercises = objectMapper.createArrayNode();
        ArrayNode answers = objectMapper.createArrayNode();
        ArrayNode analysis = objectMapper.createArrayNode();

        String[][] samples = generateGrammarExercises(grammar[0], vocab, dayNumber);
        for (String[] sample : samples) {
            exercises.add(sample[0]); // 含空格的句子
            answers.add(sample[1]);   // 答案
            analysis.add(sample[2]);  // 解释
        }

        node.set("exercises", exercises);
        node.set("answers", answers);
        node.set("analysis", analysis);
        return node;
    }

    private ObjectNode generateSpeakingSection(String[][] vocab, String[] weekTheme,
                                                String[] dailyTopic, int weekIdx, int dayIdx) {
        ObjectNode node = objectMapper.createObjectNode();

        // 使用6个词汇单词构建带空格的模板
        String[][] selectedVocab = pickRandomVocab(vocab, 6);
        String template = buildSpeakingTemplate(selectedVocab, dailyTopic, weekIdx, dayIdx);
        node.put("template", template);

        // 关键短语
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

        // 使用阅读材料的简短版本作为示例
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

    // ==================== 网页抓取 ====================

    private String[] scrapeReadingPassage(String topic, int dayNumber) {
        // 尝试多个来源
        String[] result = tryScrape(topic);
        if (result != null) return result;

        // 回退：从模板生成
        return generateFallbackPassage(topic, dayNumber);
    }

    private String[] tryScrape(String topic) {
        try {
            // 搜索breakingnewsenglish.com的分级阅读
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
                    // 找到潜在文章，尝试获取内容
                    String[] article = scrapeArticle(href);
                    if (article != null) return article;
                }
            }
        } catch (Exception e) {
            log.debug("为话题 '{}' 抓取失败: {}", topic, e.getMessage());
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
                // 尝试通用段落选择
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

            // 简单的翻译说明（真实翻译需要API）
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

    // ==================== 回退内容生成 ====================

    private String[] generateFallbackPassage(String topic, int dayNumber) {
        // 从词汇和话题构建文章
        String[] passages = {
            // 示例文章模板
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

    // ==================== 问题生成器 ====================

    private String[][] generateReadingQuestions(String passage) {
        String[] sentences = passage.split("(?<=[.!?])\\s+");
        List<String[]> questions = new ArrayList<>();

        for (int i = 0; i < sentences.length && questions.size() < 3; i++) {
            String sentence = sentences[i].trim();
            if (sentence.length() < 20) continue;

            // 根据句子结构生成WH问题和多项选择选项
            String[] q = generateQuestionFromSentence(sentence);
            if (q != null) questions.add(q);
        }

        // 如果不够，添加通用问题
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

        // 根据句子结构生成简单的WH问题
        String lower = sentence.toLowerCase();

        if (lower.contains(" is ") || lower.contains(" are ")) {
            // "X is Y" -> "What is X?"
            int isIdx = lower.indexOf(" is ");
            if (isIdx == -1) isIdx = lower.indexOf(" are ");
            String subject = sentence.substring(0, isIdx).trim();
            String predicate = sentence.substring(isIdx + (lower.contains(" is ") ? 4 : 5)).trim();

            // 移除尾部标点
            predicate = predicate.replaceAll("[.!?]$", "");

            // 创建错误选项
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

        // 通用：询问句子内容
        String keyWord = words[Math.min(words.length - 1, RANDOM.nextInt(words.length) + 2)].replaceAll("[.!?,]", "");
        return new String[]{
            "What does the passage say about " + keyWord + "?",
            "A. " + keyWord + " is important|B. " + keyWord + " is not mentioned|C. We should avoid " + keyWord,
            "A"
        };
    }

    private String[] generateListeningQuestion(String sentence, int id) {
        String lower = sentence.toLowerCase();

        // 基于句子含义的问题场景
        String question;
        String options;
        String answer;
        String keyWord;

        if (lower.contains(" is ") || lower.contains(" are ")) {
            int isIdx = lower.contains(" is ") ? lower.indexOf(" is ") : lower.indexOf(" are ");
            String subject = sentence.substring(0, isIdx).trim();
            String predicate = sentence.substring(isIdx + (lower.contains(" is ") ? 4 : 5)).trim();
            predicate = predicate.replaceAll("[.!?]$", "");

            question = "Listen: Is there " + (lower.contains(" are ") ? "are" : "is") + " " + subject + " in the passage?";
            options = "A. Yes, there is|B. No, there isn't|C. Not mentioned";
            answer = "A";
            keyWord = subject;
        } else if (lower.contains(" can ") || lower.contains(" can't ")) {
            int canIdx = lower.contains(" can ") ? lower.indexOf(" can ") : lower.indexOf(" can't ");
            String subject = sentence.substring(0, canIdx).trim();
            String rest = sentence.substring(canIdx).trim();

            question = "Listen: What does " + subject + " do in the passage?";
            options = "A. " + rest + "|B. Nothing|C. Not mentioned";
            answer = "A";
            keyWord = rest;
        } else {
            String[] words = sentence.split("\\s+");
            String keyword = words[Math.min(words.length - 1, RANDOM.nextInt(words.length) + 2)].replaceAll("[.!?,]", "");

            question = "Listen: Does the passage mention " + keyword + "?";
            options = "A. Yes|B. No|C. Not mentioned";
            answer = "A";
            keyWord = keyword;
        }

        return new String[]{
            question,
            sentence,
            options,
            answer,
            keyWord
        };
    }

    // ==================== 辅助方法 ====================

    private String[][] pickRandomVocab(String[][] vocab, int count) {
        String[][] result = new String[count][];
        Random random = new Random();
        for (int i = 0; i < count; i++) {
            result[i] = vocab[random.nextInt(vocab.length)];
        }
        return result;
    }

    private String buildSpeakingTemplate(String[][] selectedVocab, String[] dailyTopic, int weekIdx, int dayIdx) {
        StringBuilder template = new StringBuilder();
        template.append("Talk about ").append(dailyTopic[1]).append(".\n\n");
        template.append("I like to ").append(selectedVocab[0][0]).append(" ... \n");
        template.append("My favorite ").append(dailyTopic[0]).append(" is ").append(selectedVocab[1][0]).append(".\n");
        template.append("I usually ").append(selectedVocab[2][0]).append(" in the ").append(selectedVocab[3][0]).append(".\n");
        template.append("My friend ").append(selectedVocab[4][0]).append(" and I ").append(selectedVocab[5][0]).append(" together.\n");
        return template.toString();
    }

    private String[][] generateGrammarExercises(String grammarPoint, String[][] vocab, int dayNumber) {
        Random random = new Random();
        String[][] exercises = new String[5][3];

        // 生成5个填空练习
        for (int i = 0; i < 5; i++) {
            String[] v = vocab[random.nextInt(vocab.length)];
            exercises[i][0] = "I " + v[0] + " ..."; // 句子含空格
            exercises[i][1] = v[0]; // 答案
            exercises[i][2] = "使用词汇 '" + v[0] + "' (" + v[1] + ")"; // 解释
        }

        return exercises;
    }

    /**
     * 确保今天的内容存在。如果不存在，则生成。
     */
    public boolean ensureToday(LocalDate today, int dayNumber) {
        if (dayNumber < 1 || dayNumber > 35) {
            log.info("第{}天超出了35天计划范围。", dayNumber);
            return false;
        }
        return generateDay(today, dayNumber);
    }
}
