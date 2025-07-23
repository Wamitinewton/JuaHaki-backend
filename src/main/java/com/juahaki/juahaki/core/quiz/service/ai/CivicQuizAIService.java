package com.juahaki.juahaki.core.quiz.service.ai;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.juahaki.juahaki.core.quiz.mapper.QuizEntityMapper;
import com.juahaki.juahaki.core.quiz.model.CivicQuestion;
import com.juahaki.juahaki.core.quiz.model.DailyQuiz;
import com.juahaki.juahaki.core.quiz.repository.CivicQuestionRepository;
import com.juahaki.juahaki.core.quiz.repository.DailyQuizRepository;
import com.juahaki.juahaki.infrastructure.config.QuizConfigurationProperties;
import com.juahaki.juahaki.shared.utils.quiz.QuizAIBuilder;
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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

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

    private final QuizEntityMapper entityMapper;

    @Transactional
    @CacheEvict(value = {"dailyQuiz", "quizInfo", "generatedQuiz", "quizQuestions"}, allEntries = true)
    public DailyQuiz generateDailyQuiz(LocalDate quizDate, int questionCount) {
        log.info("Generating quiz for date: {} with {} questions", quizDate, questionCount);

        try {
            if (dailyQuizRepository.existsByQuizDate(quizDate)) {
                throw new RuntimeException("Quiz already exists for date: " + quizDate);
            }

            long epochDay = quizDate.toEpochDay();
            QuizConfigurationProperties.SearchStrategy strategy = quizConfig.getSearchStrategyForDate(epochDay);
            List<String> targetChapters = QuizAIBuilder.selectTargetChapters(quizDate, quizConfig.getConstitutionalChapters());

            String context = buildQuizContext(strategy.getQueries(), targetChapters);
            String focusAreas = strategy.getName();

            QuizGenerationResponse quizResponse = generateQuizWithAI(
                    context, quizDate, questionCount, focusAreas, targetChapters);

            QuizAIBuilder.validateQuizResponse(quizResponse, questionCount, "quiz generation");

            DailyQuiz dailyQuiz = entityMapper.createDailyQuiz(quizDate, quizResponse);
            DailyQuiz savedQuiz = dailyQuizRepository.save(dailyQuiz);

            List<CivicQuestion> questions = entityMapper.createQuestions(savedQuiz, quizResponse.getQuestions());
            civicQuestionRepository.saveAll(questions);

            log.info("Successfully generated quiz using strategy '{}' with ID: {} for date: {}",
                    strategy.getName(), savedQuiz.getId(), quizDate);
            return savedQuiz;

        } catch (Exception e) {
            log.error("Failed to generate quiz for date {}: {}", quizDate, e.getMessage(), e);
            throw new RuntimeException("Failed to generate quiz: " + e.getMessage(), e);
        }
    }

    /**
     * Build context for quiz generation using vector similarity search
     */
    private String buildQuizContext(List<String> searchQueries, List<String> targetChapters) {
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
                searchQueries, targetChapters, LocalDate.now(), maxDocs, threshold, searchFunction);
    }

    /**
     * Generate quiz using AI with strategic approach
     */
    private QuizGenerationResponse generateQuizWithAI(String context, LocalDate quizDate,
                                                      int questionCount, String focusAreas,
                                                      List<String> targetChapters) {
        try {
            // Build prompt variables
            Map<String, Object> promptVariables = QuizAIBuilder.buildPromptVariables(
                    context, quizDate, questionCount, focusAreas, targetChapters);

            // Create prompt
            String promptText = promptTemplateService.buildPrompt(promptVariables);
            PromptTemplate promptTemplate = new PromptTemplate(promptText);
            Prompt prompt = promptTemplate.create(promptVariables);

            // Call AI service
            String response = chatModel.call(prompt).getResult().getOutput().getText();
            log.debug("Received AI response for quiz generation, length: {}", response.length());

            // Clean and parse response
            String cleanedResponse = QuizAIBuilder.cleanJsonResponse(response);
            return objectMapper.readValue(cleanedResponse, QuizGenerationResponse.class);

        } catch (JsonProcessingException e) {
            log.error("Failed to parse AI response as JSON: {}", e.getMessage());
            throw new RuntimeException("Invalid quiz format generated by AI", e);
        } catch (Exception e) {
            log.error("Failed to generate quiz with AI: {}", e.getMessage(), e);
            throw new RuntimeException("AI quiz generation failed", e);
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
    }
}