package com.juahaki.juahaki.service.quiz;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.CacheEvict;
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

    @Value("${app.quiz.max-context-documents:15}")
    private int maxContextDocuments;

    @Value("${app.quiz.similarity-threshold:0.6}")
    private double similarityThreshold;

    private static final List<List<String>> DAILY_SEARCH_STRATEGIES = Arrays.asList(
            Arrays.asList(
                    "fundamental rights bill of rights",
                    "freedom of expression assembly",
                    "right to life liberty security",
                    "equality non-discrimination",
                    "property rights protection"),
            Arrays.asList(
                    "executive president cabinet ministers",
                    "parliament national assembly senate procedures",
                    "judiciary court system magistrates",
                    "separation of powers checks balances",
                    "independent commissions functions"),
            Arrays.asList(
                    "elections voting procedures registration",
                    "political parties formation regulation",
                    "electoral boundaries delimitation",
                    "election disputes resolution tribunals",
                    "campaign financing disclosure"),
            Arrays.asList(
                    "national budget preparation approval",
                    "taxation revenue collection",
                    "public debt borrowing limits",
                    "county revenue sharing formula",
                    "audit accountability transparency"),
            Arrays.asList(
                    "court procedures jurisdiction hierarchy",
                    "legal representation access justice",
                    "criminal justice system procedures",
                    "civil procedures dispute resolution",
                    "legal aid services provision"),
            Arrays.asList(
                    "county governments devolution",
                    "county assembly functions",
                    "governor powers responsibilities",
                    "inter-governmental relations",
                    "public participation citizen engagement"),
            Arrays.asList(
                    "public service commission functions",
                    "ethics integrity public officers",
                    "recruitment promotion procedures",
                    "disciplinary procedures misconduct",
                    "performance management evaluation"));

    private static final List<String> CONSTITUTIONAL_CHAPTERS = Arrays.asList(
            "sovereignty people chapter 1",
            "republic chapter 2",
            "citizenship chapter 3",
            "bill of rights chapter 4",
            "land environment chapter 5",
            "leadership integrity chapter 6",
            "representation people chapter 7",
            "legislature chapter 8",
            "executive chapter 9",
            "judiciary chapter 10",
            "devolved government chapter 11",
            "public finance chapter 12",
            "public service chapter 13",
            "national security chapter 14",
            "commissions independent offices chapter 15",
            "amendment constitution chapter 16",
            "general provisions chapter 17",
            "transitional provisions chapter 18");

    private static final String DIVERSE_QUIZ_PROMPT = """
            You are an expert civic educator creating a diverse daily quiz for Kenyan citizens.

            DIVERSITY REQUIREMENTS:
            1. Use the provided context to create questions from DIFFERENT constitutional sections/articles
            2. Today's focus areas: {focusAreas}
            3. Ensure questions reference SPECIFIC constitutional articles/sections
            4. Cover multiple difficulty levels and diverse scenarios
            5. Include both well-known and lesser-known constitutional provisions

            CONTEXT FROM DIFFERENT DOCUMENT SECTIONS:
            {context}

            SPECIFIC INSTRUCTIONS:
            - Generate exactly {questionCount} questions
            - Each question MUST reference a specific constitutional article/section
            - Use different chapters: {targetChapters}
            - Include practical, real-world scenarios
            - Mix theoretical knowledge with application
            - Ensure questions are from different document pages/sections

            QUESTION DISTRIBUTION:
            - {difficultyDistribution}
            - Cover: {focusAreas}
            - Reference multiple constitutional chapters

            Generate for date: {quizDate}

            Return valid JSON only:
            {{
                "title": "Daily Civic Knowledge Quiz - {quizDate}",
                "description": "Exploring: {focusAreas}",
                "questions": [
                    {{
                        "questionNumber": 1,
                        "questionText": "Question with specific scenario and constitutional reference",
                        "category": "Category name",
                        "difficulty": "Easy/Medium/Hard",
                        "options": [
                            {{"letter": "A", "text": "Option A"}},
                            {{"letter": "B", "text": "Option B"}},
                            {{"letter": "C", "text": "Option C"}},
                            {{"letter": "D", "text": "Option D"}}
                        ],
                        "correctAnswer": "A",
                        "explanation": "Detailed explanation with constitutional reference",
                        "sourceReference": "Article X, Section Y of the Constitution of Kenya 2010"
                    }}
                ]
            }}
            """;

    @Transactional
    @CacheEvict(value = { "dailyQuiz", "quizInfo", "generatedQuiz", "quizQuestions" }, allEntries = true)
    public DailyQuiz generateDailyQuiz(LocalDate quizDate, int questionCount) {
        log.info("Generating diverse quiz for date: {} with {} questions", quizDate, questionCount);

        try {
            int strategyIndex = (int) (quizDate.toEpochDay() % DAILY_SEARCH_STRATEGIES.size());
            List<String> todaysSearches = DAILY_SEARCH_STRATEGIES.get(strategyIndex);

            List<String> targetChapters = selectTargetChapters(quizDate);

            String context = getDiverseContext(todaysSearches, targetChapters, quizDate);

            String focusAreas = determineFocusAreas(strategyIndex);

            QuizGenerationResponse quizResponse = generateQuizWithStrategy(
                    context, quizDate, questionCount, focusAreas, targetChapters);

            validateQuizResponse(quizResponse, questionCount);

            DailyQuiz dailyQuiz = createDailyQuiz(quizDate, quizResponse);
            DailyQuiz savedQuiz = dailyQuizRepository.save(dailyQuiz);

            List<CivicQuestion> questions = createQuestions(savedQuiz, quizResponse.getQuestions());
            civicQuestionRepository.saveAll(questions);

            log.info("Successfully generated diverse quiz using strategy {} with ID: {} for date: {}",
                    strategyIndex, savedQuiz.getId(), quizDate);
            return savedQuiz;

        } catch (Exception e) {
            log.error("Failed to generate diverse quiz for date {}: {}", quizDate, e.getMessage(), e);
            throw new RuntimeException("Failed to generate diverse quiz: " + e.getMessage(), e);
        }
    }

    private List<String> selectTargetChapters(LocalDate quizDate) {
        List<String> shuffledChapters = new ArrayList<>(CONSTITUTIONAL_CHAPTERS);

        Collections.shuffle(shuffledChapters, new Random(quizDate.toEpochDay()));

        return shuffledChapters.subList(0, Math.min(4, shuffledChapters.size()));
    }

    private String getDiverseContext(List<String> searchQueries, List<String> targetChapters, LocalDate quizDate) {
        StringBuilder contextBuilder = new StringBuilder();
        Random dayRandom = new Random(quizDate.toEpochDay());

        for (String query : searchQueries) {
            SearchRequest searchRequest = SearchRequest.builder()
                    .query(query)
                    .topK(maxContextDocuments / searchQueries.size())
                    .similarityThreshold(similarityThreshold - 0.05)
                    .build();

            List<Document> documents = vectorStore.similaritySearch(searchRequest);

            Collections.shuffle(documents, dayRandom);

            for (Document doc : documents) {
                contextBuilder.append("Source: ")
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

        for (String chapter : targetChapters) {
            SearchRequest chapterRequest = SearchRequest.builder()
                    .query(chapter)
                    .topK(2)
                    .similarityThreshold(similarityThreshold)
                    .build();

            List<Document> chapterDocs = vectorStore.similaritySearch(chapterRequest);
            for (Document doc : chapterDocs) {
                contextBuilder.append("Constitutional Reference: ")
                        .append(chapter)
                        .append("\n")
                        .append(doc.getText())
                        .append("\n\n");
            }
        }

        String context = contextBuilder.toString();
        log.debug("Built diverse context with {} characters using {} queries and {} chapters",
                context.length(), searchQueries.size(), targetChapters.size());

        return context;
    }

    private String determineFocusAreas(int strategyIndex) {
        List<String> focusDescriptions = Arrays.asList(
                "Constitutional Rights and Fundamental Freedoms",
                "Government Structure and Separation of Powers",
                "Electoral Democracy and Political Participation",
                "Public Finance and Resource Management",
                "Legal System and Access to Justice",
                "Devolution and County Government",
                "Public Service and Ethics");

        return focusDescriptions.get(strategyIndex % focusDescriptions.size());
    }

    private QuizGenerationResponse generateQuizWithStrategy(String context, LocalDate quizDate,
            int questionCount, String focusAreas, List<String> targetChapters) {

        try {
            String difficultyDistribution = String.format(
                    "%d Easy, %d Medium, %d Hard",
                    questionCount / 3, questionCount / 3, questionCount - (2 * (questionCount / 3)));

            PromptTemplate promptTemplate = new PromptTemplate(DIVERSE_QUIZ_PROMPT);

            Map<String, Object> promptVariables = Map.of(
                    "context", context != null ? context : "",
                    "quizDate", quizDate.toString(),
                    "questionCount", String.valueOf(questionCount),
                    "difficultyDistribution", difficultyDistribution,
                    "focusAreas", focusAreas,
                    "targetChapters", String.join(", ", targetChapters));

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

    public Map<String, Object> getQuizDiversityInfo(LocalDate date) {
        int strategyIndex = (int) (date.toEpochDay() % DAILY_SEARCH_STRATEGIES.size());
        List<String> targetChapters = selectTargetChapters(date);
        String focusAreas = determineFocusAreas(strategyIndex);

        return Map.of(
                "date", date.toString(),
                "strategyIndex", strategyIndex,
                "focusAreas", focusAreas,
                "targetChapters", targetChapters,
                "searchQueries", DAILY_SEARCH_STRATEGIES.get(strategyIndex),
                "totalStrategies", DAILY_SEARCH_STRATEGIES.size());
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