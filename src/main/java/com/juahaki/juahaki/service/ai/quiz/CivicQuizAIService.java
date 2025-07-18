package com.juahaki.juahaki.service.ai.quiz;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.juahaki.juahaki.config.QuizConfigurationProperties;
import com.juahaki.juahaki.mapper.QuizEntityMapper;
import com.juahaki.juahaki.model.quiz.CivicQuestion;
import com.juahaki.juahaki.model.quiz.DailyQuiz;
import com.juahaki.juahaki.repository.quiz.CivicQuestionRepository;
import com.juahaki.juahaki.repository.quiz.DailyQuizRepository;
import com.juahaki.juahaki.util.quiz.QuizAIBuilder;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.chat.prompt.PromptTemplate;
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

    private final QuizEntityMapper entityMapper;

    private static final String QUIZ_QUESTIONS_PREFIX = "quiz:questions:";

    @Transactional
    @CacheEvict(value = { "dailyQuiz", "quizInfo", "generatedQuiz", "quizQuestions" }, allEntries = true)
    public DailyQuiz generateDailyQuiz(LocalDate quizDate, int questionCount) {
        log.info("Generating diverse quiz for date: {} with {} questions", quizDate, questionCount);

        try {
            long epochDay = quizDate.toEpochDay();
            QuizConfigurationProperties.SearchStrategy strategy = quizConfig.getSearchStrategyForDate(epochDay);
            List<String> targetChapters = QuizAIBuilder.selectTargetChapters(quizDate, quizConfig.getConstitutionalChapters());

            String context = buildDiverseContext(strategy.getQueries(), targetChapters, quizDate);
            String focusAreas = strategy.getName();

            QuizGenerationResponse quizResponse = generateQuizWithStrategy(
                    context, quizDate, questionCount, focusAreas, targetChapters);

            QuizAIBuilder.validateQuizResponse(quizResponse, questionCount, "quiz generation");

            DailyQuiz dailyQuiz = entityMapper.createDailyQuiz(quizDate, quizResponse);
            DailyQuiz savedQuiz = dailyQuizRepository.save(dailyQuiz);

            List<CivicQuestion> questions = entityMapper.createQuestions(savedQuiz, quizResponse.getQuestions());
            List<CivicQuestion> savedQuestions = civicQuestionRepository.saveAll(questions);

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
     * Build diverse context using AI builder utility
     */
    private String buildDiverseContext(List<String> searchQueries, List<String> targetChapters, LocalDate quizDate) {
        int maxDocs = quizConfig.getGeneration().getMaxContextDocuments();
        double threshold = quizConfig.getGeneration().getSimilarityThreshold();

        // Create search function
        QuizAIBuilder.ContextSearchFunction searchFunction = (query, limit, searchThreshold) -> {
            SearchRequest searchRequest = SearchRequest.builder()
                    .query(query)
                    .topK(limit)
                    .similarityThreshold(searchThreshold)
                    .build();
            return vectorStore.similaritySearch(searchRequest);
        };

        return QuizAIBuilder.buildDiverseContext(
                searchQueries, targetChapters, quizDate, maxDocs, threshold, searchFunction);
    }

    /**
     * Generate quiz using AI with strategic approach
     */
    private QuizGenerationResponse generateQuizWithStrategy(String context, LocalDate quizDate,
                                                            int questionCount, String focusAreas,
                                                            List<String> targetChapters) {
        try {
            Map<String, Object> promptVariables = QuizAIBuilder.buildPromptVariables(
                    context, quizDate, questionCount, focusAreas, targetChapters);

            String promptText = promptTemplateService.buildPrompt(promptVariables);
            PromptTemplate promptTemplate = new PromptTemplate(promptText);
            Prompt prompt = promptTemplate.create(promptVariables);

            String response = chatModel.call(prompt).getResult().getOutput().getText();
            log.debug("Received AI response for diverse quiz generation, length: {}", response.length());

            String cleanedResponse = QuizAIBuilder.cleanJsonResponse(response);
            return objectMapper.readValue(cleanedResponse, QuizGenerationResponse.class);

        } catch (JsonProcessingException e) {
            log.error("Failed to parse AI response as JSON: {}", e.getMessage());
            throw new RuntimeException("Invalid quiz format generated by AI", e);
        } catch (Exception e) {
            log.error("Failed to generate diverse quiz with AI: {}", e.getMessage(), e);
            throw new RuntimeException("AI quiz generation failed", e);
        }
    }

    /**
     * Cache quiz questions in Redis when they are first generated
     */
    private void cacheQuestionsInRedis(DailyQuiz quiz, List<CivicQuestion> questions) {
        String questionsKey = QUIZ_QUESTIONS_PREFIX + quiz.getId();

        try {
            Map<String, Object> questionMap = new HashMap<>();
            for (CivicQuestion question : questions) {
                var redisDto = entityMapper.convertToRedisDto(question);
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
    }}