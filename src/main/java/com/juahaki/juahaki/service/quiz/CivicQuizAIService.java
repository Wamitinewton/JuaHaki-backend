package com.juahaki.juahaki.service.quiz;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.juahaki.juahaki.config.QuizConfigurationProperties;
import com.juahaki.juahaki.dto.quiz.civic.RedisQuestionDto;
import com.juahaki.juahaki.model.quiz.CivicQuestion;
import com.juahaki.juahaki.model.quiz.DailyQuiz;
import com.juahaki.juahaki.model.quiz.QuestionOption;
import com.juahaki.juahaki.repository.quiz.CivicQuestionRepository;
import com.juahaki.juahaki.repository.quiz.DailyQuizRepository;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class CivicQuizAIService {

    private final ChatModel chatModel;
    private final VectorStore vectorStore;
    private final DailyQuizRepository dailyQuizRepository;
    private final CivicQuestionRepository civicQuestionRepository;
    private final ObjectMapper objectMapper;
    private final QuizConfigurationProperties quizConfig;
    private final PromptTemplateService promptTemplateService;
    private final RedisTemplate<String, Object> redisTemplate;

    private static final String QUIZ_QUESTIONS_PREFIX = "quiz:questions:";

    @Transactional
    @CacheEvict(value = { "dailyQuiz", "quizInfo", "generatedQuiz", "quizQuestions" }, allEntries = true)
    public DailyQuiz generateDailyQuiz(LocalDate quizDate, int questionCount) {
        log.info("Generating diverse quiz for date: {} with {} questions", quizDate, questionCount);

        try {
            long epochDay = quizDate.toEpochDay();
            QuizConfigurationProperties.SearchStrategy strategy = quizConfig.getSearchStrategyForDate(epochDay);
            List<String> targetChapters = selectTargetChapters(quizDate);

            String context = getDiverseContext(strategy.getQueries(), targetChapters, quizDate);
            String focusAreas = strategy.getName();

            QuizGenerationResponse quizResponse = generateQuizWithStrategy(
                    context, quizDate, questionCount, focusAreas, targetChapters);

            validateQuizResponse(quizResponse, questionCount);

            DailyQuiz dailyQuiz = createDailyQuiz(quizDate, quizResponse);
            DailyQuiz savedQuiz = dailyQuizRepository.save(dailyQuiz);

            List<CivicQuestion> questions = createQuestions(savedQuiz, quizResponse.getQuestions());
            List<CivicQuestion> savedQuestions = civicQuestionRepository.saveAll(questions);

            // Cache questions in Redis immediately after saving to database
            cacheQuestionsInRedis(savedQuiz, savedQuestions);

            log.info("Successfully generated diverse quiz using strategy '{}' with ID: {} for date: {}",
                    strategy.getName(), savedQuiz.getId(), quizDate);
            return savedQuiz;

        } catch (Exception e) {
            log.error("Failed to generate diverse quiz for date {}: {}", quizDate, e.getMessage(), e);
            throw new RuntimeException("Failed to generate diverse quiz: " + e.getMessage(), e);
        }
    }

    /**
     * Cache quiz questions in Redis when they are first generated
     */
    private void cacheQuestionsInRedis(DailyQuiz quiz, List<CivicQuestion> questions) {
        String questionsKey = QUIZ_QUESTIONS_PREFIX + quiz.getId();

        try {
            // Convert to Redis-friendly DTOs and store each question with its number as a hash field
            Map<String, Object> questionMap = new HashMap<>();
            for (CivicQuestion question : questions) {
                RedisQuestionDto redisDto = convertToRedisDto(question);
                questionMap.put(String.valueOf(question.getQuestionNumber()), redisDto);
            }

            redisTemplate.opsForHash().putAll(questionsKey, questionMap);

            Duration timeUntilExpiry = Duration.between(LocalDateTime.now(), quiz.getExpiresAt());
            if (!timeUntilExpiry.isNegative() && !timeUntilExpiry.isZero()) {
                redisTemplate.expire(questionsKey, timeUntilExpiry);
            }

            log.info("Cached {} questions for new quiz {} in Redis with expiry: {}",
                    questions.size(), quiz.getId(), quiz.getExpiresAt());

        } catch (Exception e) {
            log.error("Failed to cache questions for new quiz {} in Redis: {}", quiz.getId(), e.getMessage(), e);
        }
    }

    private RedisQuestionDto convertToRedisDto(CivicQuestion question) {
        List<RedisQuestionDto.RedisOptionDto> optionDtos = question.getOptions().stream()
                .map(option -> RedisQuestionDto.RedisOptionDto.builder()
                        .optionLetter(option.getOptionLetter())
                        .optionText(option.getOptionText())
                        .build())
                .collect(Collectors.toList());

        return RedisQuestionDto.builder()
                .questionId(question.getId())
                .questionNumber(question.getQuestionNumber())
                .questionText(question.getQuestionText())
                .explanation(question.getExplanation())
                .category(question.getCategory())
                .difficulty(question.getDifficulty())
                .correctAnswer(question.getCorrectAnswer())
                .sourceReference(question.getSourceReference())
                .options(optionDtos)
                .build();
    }

    private List<String> selectTargetChapters(LocalDate quizDate) {
        // Create a mutable copy of the constitutional chapters
        List<String> shuffledChapters = new ArrayList<>(quizConfig.getConstitutionalChapters());
        Collections.shuffle(shuffledChapters, new Random(quizDate.toEpochDay()));
        return shuffledChapters.subList(0, Math.min(4, shuffledChapters.size()));
    }

    private String getDiverseContext(List<String> searchQueries, List<String> targetChapters, LocalDate quizDate) {
        StringBuilder contextBuilder = new StringBuilder();
        Random dayRandom = new Random(quizDate.toEpochDay());

        int maxDocs = quizConfig.getGeneration().getMaxContextDocuments();
        double threshold = quizConfig.getGeneration().getSimilarityThreshold();

        // Search using daily queries
        for (String query : searchQueries) {
            SearchRequest searchRequest = SearchRequest.builder()
                    .query(query)
                    .topK(maxDocs / searchQueries.size())
                    .similarityThreshold(threshold - 0.05)
                    .build();

            List<Document> documents = vectorStore.similaritySearch(searchRequest);
            List<Document> mutableDocuments = new ArrayList<>(documents);
            Collections.shuffle(mutableDocuments, dayRandom);

            appendDocumentsToContext(contextBuilder, mutableDocuments, "Search Query: " + query);
        }

        // Search using constitutional chapters
        for (String chapter : targetChapters) {
            SearchRequest chapterRequest = SearchRequest.builder()
                    .query(chapter)
                    .topK(2)
                    .similarityThreshold(threshold)
                    .build();

            List<Document> chapterDocs = vectorStore.similaritySearch(chapterRequest);
            appendDocumentsToContext(contextBuilder, chapterDocs, "Constitutional Reference: " + chapter);
        }

        String context = contextBuilder.toString();
        log.debug("Built diverse context with {} characters using {} queries and {} chapters",
                context.length(), searchQueries.size(), targetChapters.size());

        return context;
    }

    private void appendDocumentsToContext(StringBuilder contextBuilder, List<Document> documents, String sourceLabel) {
        for (Document doc : documents) {
            contextBuilder.append(sourceLabel)
                    .append(" | Source: ")
                    .append(doc.getMetadata().getOrDefault("source", "Unknown"))
                    .append(" | Page: ")
                    .append(doc.getMetadata().getOrDefault("page_number", "N/A"))
                    .append(" | Section: ")
                    .append(doc.getMetadata().getOrDefault("chunk_index", "N/A"))
                    .append("\n")
                    .append(doc.getText())
                    .append("\n\n");
        }
    }

    private QuizGenerationResponse generateQuizWithStrategy(String context, LocalDate quizDate,
                                                            int questionCount, String focusAreas, List<String> targetChapters) {

        try {
            String difficultyDistribution = calculateDifficultyDistribution(questionCount);

            Map<String, Object> promptVariables = Map.of(
                    "context", context != null ? context : "",
                    "quizDate", quizDate.toString(),
                    "questionCount", String.valueOf(questionCount),
                    "difficultyDistribution", difficultyDistribution,
                    "focusAreas", focusAreas,
                    "targetChapters", String.join(", ", targetChapters));

            String promptText = promptTemplateService.buildPrompt(promptVariables);
            PromptTemplate promptTemplate = new PromptTemplate(promptText);
            Prompt prompt = promptTemplate.create(promptVariables);

            String response = chatModel.call(prompt).getResult().getOutput().getText();
            log.debug("Received AI response for diverse quiz generation, length: {}", response.length());

            String cleanedResponse = cleanJsonResponse(response);
            return objectMapper.readValue(cleanedResponse, QuizGenerationResponse.class);

        } catch (JsonProcessingException e) {
            log.error("Failed to parse AI response as JSON: {}", e.getMessage());
            throw new RuntimeException("Invalid quiz format generated by AI", e);
        } catch (Exception e) {
            log.error("Failed to generate diverse quiz with AI: {}", e.getMessage(), e);
            throw new RuntimeException("AI quiz generation failed", e);
        }
    }

    private String calculateDifficultyDistribution(int questionCount) {
        int easy = questionCount / 3;
        int medium = questionCount / 3;
        int hard = questionCount - (easy + medium);
        return String.format("%d Easy, %d Medium, %d Hard", easy, medium, hard);
    }

    public Map<String, Object> getQuizDiversityInfo(LocalDate date) {
        long epochDay = date.toEpochDay();
        QuizConfigurationProperties.SearchStrategy strategy = quizConfig.getSearchStrategyForDate(epochDay);
        List<String> targetChapters = selectTargetChapters(date);

        return Map.of(
                "date", date.toString(),
                "strategyName", strategy.getName(),
                "focusAreas", strategy.getName(),
                "targetChapters", targetChapters,
                "searchQueries", strategy.getQueries(),
                "totalStrategies", quizConfig.getDailySearchStrategies().size());
    }

    private String cleanJsonResponse(String response) {
        String cleaned = response.trim();
        if (cleaned.startsWith("```json")) {
            cleaned = cleaned.substring(7);
        }
        if (cleaned.startsWith("```")) {
            cleaned = cleaned.substring(3);
        }
        if (cleaned.endsWith("```")) {
            cleaned = cleaned.substring(0, cleaned.length() - 3);
        }
        return cleaned.trim();
    }

    private void validateQuizResponse(QuizGenerationResponse response, int expectedQuestionCount) {
        if (response == null || response.getQuestions() == null || response.getQuestions().isEmpty()) {
            throw new IllegalArgumentException("No questions generated");
        }
        log.info("Quiz validation passed for {} questions", response.getQuestions().size());
    }

    private DailyQuiz createDailyQuiz(LocalDate quizDate, QuizGenerationResponse response) {
        return DailyQuiz.builder()
                .quizDate(quizDate)
                .title(response.getTitle())
                .description(response.getDescription())
                .totalQuestions(response.getQuestions().size())
                .isActive(true)
                .expiresAt(quizDate.plusDays(1).atStartOfDay())
                .build();
    }

    private List<CivicQuestion> createQuestions(DailyQuiz quiz, List<QuestionData> questionData) {
        return questionData.stream().map(data -> {
            List<QuestionOption> options = data.getOptions().stream()
                    .map(optionData -> QuestionOption.builder()
                            .optionLetter(optionData.getLetter())
                            .optionText(optionData.getText())
                            .build())
                    .collect(Collectors.toList());

            CivicQuestion question = CivicQuestion.builder()
                    .dailyQuiz(quiz)
                    .questionNumber(data.getQuestionNumber())
                    .questionText(data.getQuestionText())
                    .explanation(data.getExplanation())
                    .category(data.getCategory())
                    .difficulty(data.getDifficulty())
                    .correctAnswer(data.getCorrectAnswer())
                    .sourceReference(data.getSourceReference())
                    .build();

            options.forEach(option -> option.setQuestion(question));
            question.setOptions(options);
            return question;
        }).collect(Collectors.toList());
    }

    @Getter
    @Setter
    public static class QuizGenerationResponse {
        private String title;
        private String description;
        private List<QuestionData> questions;
    }

    @Getter
    public static class QuestionData {
        private Integer questionNumber;
        private String questionText;
        private String category;
        private String difficulty;
        private List<OptionData> options;
        private String correctAnswer;
        private String explanation;
        private String sourceReference;
    }

    @Getter
    public static class OptionData {
        private String letter;
        private String text;
    }
}