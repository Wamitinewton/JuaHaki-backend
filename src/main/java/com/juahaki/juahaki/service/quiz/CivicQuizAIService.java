package com.juahaki.juahaki.service.quiz;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.juahaki.juahaki.model.quiz.CivicQuestion;
import com.juahaki.juahaki.model.quiz.DailyQuiz;
import com.juahaki.juahaki.model.quiz.QuestionOption;
import com.juahaki.juahaki.repository.quiz.CivicQuestionRepository;
import com.juahaki.juahaki.repository.quiz.DailyQuizRepository;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
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

    @Value("${app.quiz.max-context-documents:10}")
    private int maxContextDocuments;

    @Value("${app.quiz.similarity-threshold:0.6}")
    private double similarityThreshold;

    private static final String QUIZ_GENERATION_PROMPT = """
            You are an expert civic educator specializing in Kenyan governance, constitution, and public policy.
            
            Your task is to create a comprehensive daily civic quiz that educates Kenyan citizens about their rights,
            governance structures, constitutional provisions, and civic responsibilities.
            
            CONTEXT AND REFERENCE MATERIALS:
            {context}
            
            QUIZ REQUIREMENTS:
            - Generate exactly {questionCount} multiple-choice questions
            - Each question must have exactly 4 options (A, B, C, D)
            - Questions should cover diverse civic topics relevant to Kenya
            - Include a mix of difficulty levels: {difficultyDistribution}
            - Ensure questions are educational and promote civic awareness
            - Focus on practical knowledge that empowers citizens
            
            TOPIC CATEGORIES TO INCLUDE:
            - Constitutional Rights and Freedoms
            - Government Structure and Functions
            - Electoral Processes and Democracy
            - Public Finance and Budgeting
            - Legal System and Justice
            - Public Policy and Legislation
            - Civic Duties and Responsibilities
            - Local Government and Devolution
            
            QUESTION QUALITY STANDARDS:
            1. Accuracy: All information must be factually correct and current
            2. Clarity: Questions should be clear and unambiguous
            3. Educational Value: Each question should teach something valuable
            4. Practical Relevance: Focus on knowledge useful for informed citizenship
            5. Cultural Sensitivity: Respect Kenya diverse cultural landscape
            6. Progressive Difficulty: Mix easy, medium, and challenging questions
            
            Generate the quiz for date: {quizDate}
            
            RESPONSE FORMAT INSTRUCTIONS:
            You must respond with a valid JSON object only. Do not include any text outside the JSON structure.
            Do not use markdown code blocks or backticks.
            
            The JSON should have this structure:
            - A title field with value: Daily Civic Knowledge Quiz - {quizDate}
            - A description field explaining the quiz purpose
            - A questions array containing exactly {questionCount} question objects
            - Each question object should have: questionNumber, questionText, category, difficulty, options array, correctAnswer, explanation, sourceReference
            - Each option object should have: letter (A/B/C/D) and text
            
            EXAMPLE QUESTION STRUCTURE:
            Question about constitutional rights with 4 options A through D, correct answer specified, detailed explanation provided, and source reference to constitution article.
            
            Generate the complete quiz now as valid JSON only.
            """;

    private static final List<String> CIVIC_CATEGORIES = Arrays.asList(
            "Constitutional Rights and Freedoms",
            "Government Structure and Functions",
            "Electoral Processes and Democracy",
            "Public Finance and Budgeting",
            "Legal System and Justice",
            "Public Policy and Legislation",
            "Civic Duties and Responsibilities",
            "Local Government and Devolution"
    );

    private static final List<String> DIFFICULTY_LEVELS = Arrays.asList("Easy", "Medium", "Hard");

    @Transactional
    @CacheEvict(value = {"dailyQuiz", "quizInfo", "generatedQuiz", "quizQuestions"}, allEntries = true)
    public DailyQuiz generateDailyQuiz(LocalDate quizDate, int questionCount) {
        log.info("Generating daily quiz for date: {} with {} questions", quizDate, questionCount);

        try {
            if (dailyQuizRepository.existsByQuizDate(quizDate)) {
                throw new IllegalArgumentException("Quiz already exists for date: " + quizDate);
            }

            String context = getRelevantContext();
            log.debug("Retrieved context with {} characters for quiz generation", context.length());

            QuizGenerationResponse quizResponse = generateQuizWithAI(context, quizDate, questionCount);

            validateQuizResponse(quizResponse, questionCount);

            DailyQuiz dailyQuiz = createDailyQuiz(quizDate, quizResponse);
            DailyQuiz savedQuiz = dailyQuizRepository.save(dailyQuiz);

            List<CivicQuestion> questions = createQuestions(savedQuiz, quizResponse.getQuestions());
            civicQuestionRepository.saveAll(questions);

            log.info("Successfully generated quiz with ID: {} for date: {}", savedQuiz.getId(), quizDate);
            return savedQuiz;

        } catch (Exception e) {
            log.error("Failed to generate daily quiz for date {}: {}", quizDate, e.getMessage(), e);
            throw new RuntimeException("Failed to generate daily quiz: " + e.getMessage(), e);
        }
    }

    @Transactional
    @CacheEvict(value = {"dailyQuiz", "quizInfo", "generatedQuiz", "quizQuestions", "leaderboard", "quizStats"}, allEntries = true)
    public DailyQuiz regenerateQuiz(LocalDate quizDate, String reason) {
        log.info("Regenerating quiz for date: {}, reason: {}", quizDate, reason);

        dailyQuizRepository.findByQuizDate(quizDate).ifPresent(existingQuiz -> {
            log.info("Deleting existing quiz for date: {}", quizDate);
            dailyQuizRepository.delete(existingQuiz);
        });

        return generateDailyQuiz(quizDate, 10);
    }

    @Cacheable(value = "quizAIContext", key = "'suggested_topics'")
    public List<String> getSuggestedTopics() {
        log.debug("Getting suggested civic topics");

        try {
            SearchRequest searchRequest = SearchRequest.builder()
                    .query("civic education governance constitution")
                    .topK(20)
                    .similarityThreshold(0.5)
                    .build();

            List<Document> documents = vectorStore.similaritySearch(searchRequest);

            return documents.stream()
                    .flatMap(doc -> extractTopicsFromDocument(doc).stream())
                    .distinct()
                    .limit(10)
                    .collect(Collectors.toList());

        } catch (Exception e) {
            log.warn("Failed to get suggested topics from vector store: {}", e.getMessage());
            return CIVIC_CATEGORIES;
        }
    }

    @Cacheable(value = "quizQuality", key = "#quizId")
    public QuizQualityAnalysis analyzeQuizQuality(Long quizId) {
        log.debug("Analyzing quiz quality for quiz ID: {}", quizId);

        DailyQuiz quiz = dailyQuizRepository.findById(quizId)
                .orElseThrow(() -> new IllegalArgumentException("Quiz not found"));

        List<CivicQuestion> questions = civicQuestionRepository.findByDailyQuizOrderByQuestionNumber(quiz);

        return QuizQualityAnalysis.builder()
                .quizId(quizId)
                .totalQuestions(questions.size())
                .categoryDistribution(analyzeCategoryDistribution(questions))
                .difficultyDistribution(analyzeDifficultyDistribution(questions))
                .averageQuestionLength(calculateAverageQuestionLength(questions))
                .hasExplanations(checkExplanationsPresent(questions))
                .hasSourceReferences(checkSourceReferencesPresent(questions))
                .qualityScore(calculateQualityScore(questions))
                .recommendations(generateQualityRecommendations(questions))
                .build();
    }

    @Cacheable(value = "quizAIContext", key = "'context_' + #searchQueries.hashCode()")
    private String getRelevantContext() {
        try {
            List<String> searchQueries = Arrays.asList(
                    "Kenya constitution rights freedoms",
                    "government structure parliament president",
                    "electoral democracy voting process",
                    "public finance budget taxation",
                    "legal system courts justice",
                    "civic duties responsibilities citizenship",
                    "county government devolution",
                    "public policy legislation laws"
            );

            StringBuilder contextBuilder = new StringBuilder();

            for (String query : searchQueries) {
                SearchRequest searchRequest = SearchRequest.builder()
                        .query(query)
                        .topK(maxContextDocuments / searchQueries.size())
                        .similarityThreshold(similarityThreshold)
                        .build();

                List<Document> documents = vectorStore.similaritySearch(searchRequest);

                for (Document doc : documents) {
                    contextBuilder.append("Source: ")
                            .append(doc.getMetadata().getOrDefault("source", "Unknown"))
                            .append("\n")
                            .append(doc.getText())
                            .append("\n\n");
                }
            }

            String context = contextBuilder.toString();
            log.debug("Built context from {} search queries, total length: {}", searchQueries.size(), context.length());

            return context;

        } catch (Exception e) {
            log.warn("Failed to retrieve context from vector store: {}", e.getMessage());
            return "";
        }
    }

    private QuizGenerationResponse generateQuizWithAI(String context, LocalDate quizDate, int questionCount) {
        try {
            Prompt prompt = createPromptTemplate(context, quizDate, questionCount);
            String response = chatModel.call(prompt).getResult().getOutput().getText();

            log.debug("Received AI response for quiz generation, length: {}", response.length());

            String cleanedResponse = cleanJsonResponse(response);
            return objectMapper.readValue(cleanedResponse, QuizGenerationResponse.class);

        } catch (JsonProcessingException e) {
            log.error("Failed to parse AI response as JSON: {}", e.getMessage());
            throw new RuntimeException("Invalid quiz format generated by AI", e);
        } catch (Exception e) {
            log.error("Failed to generate quiz with AI: {}", e.getMessage(), e);
            throw new RuntimeException("AI quiz generation failed", e);
        }
    }

    private Prompt createPromptTemplate(String context, LocalDate quizDate, int questionCount) {
        String difficultyDistribution = String.format(
                "%d Easy, %d Medium, %d Hard",
                questionCount / 3, questionCount / 3, questionCount - (2 * (questionCount / 3))
        );

        try {
            PromptTemplate promptTemplate = new PromptTemplate(QUIZ_GENERATION_PROMPT);

            Map<String, Object> promptVariables = Map.of(
                    "context", context != null ? context : "",
                    "quizDate", quizDate.toString(),
                    "questionCount", String.valueOf(questionCount),
                    "difficultyDistribution", difficultyDistribution
            );

            return promptTemplate.create(promptVariables);

        } catch (Exception e) {
            log.error("Error creating prompt template: {}", e.getMessage(), e);
            // Fallback to manual string replacement if PromptTemplate fails
            String manualPrompt = QUIZ_GENERATION_PROMPT
                    .replace("{context}", context != null ? context : "")
                    .replace("{quizDate}", quizDate.toString())
                    .replace("{questionCount}", String.valueOf(questionCount))
                    .replace("{difficultyDistribution}", difficultyDistribution);

            return new Prompt(manualPrompt);
        }
    }

    private String cleanJsonResponse(String response) {
        // Remove any Markdown code blocks or extra formatting
        String cleaned = response.trim();

        // Remove Markdown code blocks if present
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
        if (response == null) {
            throw new IllegalArgumentException("Quiz response is null");
        }

        if (response.getQuestions() == null || response.getQuestions().isEmpty()) {
            throw new IllegalArgumentException("No questions generated");
        }

        if (response.getQuestions().size() != expectedQuestionCount) {
            log.warn("Expected {} questions but got {}", expectedQuestionCount, response.getQuestions().size());
        }

        for (int i = 0; i < response.getQuestions().size(); i++) {
            QuestionData question = response.getQuestions().get(i);
            validateQuestion(question, i + 1);
        }

        log.info("Quiz validation passed for {} questions", response.getQuestions().size());
    }

    private void validateQuestion(QuestionData question, int expectedNumber) {
        if (question.getQuestionText() == null || question.getQuestionText().trim().isEmpty()) {
            throw new IllegalArgumentException("Question " + expectedNumber + " has empty text");
        }

        if (question.getOptions() == null || question.getOptions().size() != 4) {
            throw new IllegalArgumentException("Question " + expectedNumber + " must have exactly 4 options");
        }

        if (question.getCorrectAnswer() == null ||
                !Arrays.asList("A", "B", "C", "D").contains(question.getCorrectAnswer())) {
            throw new IllegalArgumentException("Question " + expectedNumber + " has invalid correct answer");
        }

        if (!CIVIC_CATEGORIES.contains(question.getCategory())) {
            log.warn("Question {} has unexpected category: {}", expectedNumber, question.getCategory());
        }

        if (!DIFFICULTY_LEVELS.contains(question.getDifficulty())) {
            log.warn("Question {} has unexpected difficulty: {}", expectedNumber, question.getDifficulty());
        }
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
        List<CivicQuestion> questions = new ArrayList<>();

        for (QuestionData data : questionData) {
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

            // Set the question reference for each option
            options.forEach(option -> option.setQuestion(question));
            question.setOptions(options);

            questions.add(question);
        }

        return questions;
    }

    private List<String> extractTopicsFromDocument(Document document) {
        String content = document.getText().toLowerCase();
        List<String> topics = new ArrayList<>();

        for (String category : CIVIC_CATEGORIES) {
            String[] keywords = category.toLowerCase().split(" ");
            boolean hasKeywords = Arrays.stream(keywords)
                    .anyMatch(content::contains);

            if (hasKeywords) {
                topics.add(category);
            }
        }

        return topics;
    }

    private Map<String, Integer> analyzeCategoryDistribution(List<CivicQuestion> questions) {
        return questions.stream()
                .collect(Collectors.groupingBy(
                        CivicQuestion::getCategory,
                        Collectors.collectingAndThen(Collectors.counting(), Math::toIntExact)
                ));
    }

    private Map<String, Integer> analyzeDifficultyDistribution(List<CivicQuestion> questions) {
        return questions.stream()
                .collect(Collectors.groupingBy(
                        CivicQuestion::getDifficulty,
                        Collectors.collectingAndThen(Collectors.counting(), Math::toIntExact)
                ));
    }

    private double calculateAverageQuestionLength(List<CivicQuestion> questions) {
        return questions.stream()
                .mapToInt(q -> q.getQuestionText().length())
                .average()
                .orElse(0.0);
    }

    private boolean checkExplanationsPresent(List<CivicQuestion> questions) {
        return questions.stream()
                .allMatch(q -> q.getExplanation() != null && !q.getExplanation().trim().isEmpty());
    }

    private boolean checkSourceReferencesPresent(List<CivicQuestion> questions) {
        return questions.stream()
                .allMatch(q -> q.getSourceReference() != null && !q.getSourceReference().trim().isEmpty());
    }

    private double calculateQualityScore(List<CivicQuestion> questions) {
        double score = 0.0;

        Set<String> uniqueCategories = questions.stream()
                .map(CivicQuestion::getCategory)
                .collect(Collectors.toSet());
        score += (uniqueCategories.size() / (double) CIVIC_CATEGORIES.size()) * 20;

        Map<String, Integer> difficultyDist = analyzeDifficultyDistribution(questions);
        boolean hasAllDifficulties = DIFFICULTY_LEVELS.stream()
                .allMatch(difficultyDist::containsKey);
        if (hasAllDifficulties) score += 20;

        if (checkExplanationsPresent(questions)) score += 25;

        if (checkSourceReferencesPresent(questions)) score += 25;

        double avgLength = calculateAverageQuestionLength(questions);
        if (avgLength >= 50 && avgLength <= 200) score += 10;

        return Math.min(score, 100.0);
    }

    private List<String> generateQualityRecommendations(List<CivicQuestion> questions) {
        List<String> recommendations = new ArrayList<>();

        if (!checkExplanationsPresent(questions)) {
            recommendations.add("Add detailed explanations to all questions for better educational value");
        }

        if (!checkSourceReferencesPresent(questions)) {
            recommendations.add("Include source references for all questions to improve credibility");
        }

        Map<String, Integer> categoryDist = analyzeCategoryDistribution(questions);
        if (categoryDist.size() < 4) {
            recommendations.add("Increase category diversity to cover more civic topics");
        }

        double avgLength = calculateAverageQuestionLength(questions);
        if (avgLength < 50) {
            recommendations.add("Consider making questions more detailed and informative");
        } else if (avgLength > 200) {
            recommendations.add("Consider making questions more concise for better readability");
        }

        return recommendations;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class QuizGenerationResponse {
        private String title;
        private String description;
        private List<QuestionData> questions;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class QuestionData {
        @JsonProperty("questionNumber")
        private Integer questionNumber;

        @JsonProperty("questionText")
        private String questionText;

        @JsonProperty("category")
        private String category;

        @JsonProperty("difficulty")
        private String difficulty;

        @JsonProperty("options")
        private List<OptionData> options;

        @JsonProperty("correctAnswer")
        private String correctAnswer;

        @JsonProperty("explanation")
        private String explanation;

        @JsonProperty("sourceReference")
        private String sourceReference;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class OptionData {
        @JsonProperty("letter")
        private String letter;

        @JsonProperty("text")
        private String text;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class QuizQualityAnalysis {
        private Long quizId;
        private int totalQuestions;
        private Map<String, Integer> categoryDistribution;
        private Map<String, Integer> difficultyDistribution;
        private double averageQuestionLength;
        private boolean hasExplanations;
        private boolean hasSourceReferences;
        private double qualityScore;
        private List<String> recommendations;
    }
}   