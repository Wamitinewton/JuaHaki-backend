package com.juahaki.juahaki.util.quiz;

import com.juahaki.juahaki.config.QuizConfigurationProperties;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;

import java.time.LocalDate;
import java.util.*;

@UtilityClass
@Slf4j
public class QuizAIBuilder {

    /**
     * Select target constitutional chapters for the quiz based on date
     */
    public List<String> selectTargetChapters(LocalDate quizDate, List<String> constitutionalChapters) {
        List<String> shuffledChapters = new ArrayList<>(constitutionalChapters);
        Collections.shuffle(shuffledChapters, new Random(quizDate.toEpochDay()));
        return shuffledChapters.subList(0, Math.min(4, shuffledChapters.size()));
    }

    /**
     * Build diverse context from search results and constitutional chapters
     */
    public String buildDiverseContext(List<String> searchQueries, List<String> targetChapters,
                                      LocalDate quizDate, int maxDocs, double threshold,
                                      ContextSearchFunction searchFunction) {
        StringBuilder contextBuilder = new StringBuilder();
        Random dayRandom = new Random(quizDate.toEpochDay());

        // Search using daily queries
        for (String query : searchQueries) {
            List<Document> documents = searchFunction.search(query, maxDocs / searchQueries.size(), threshold - 0.05);
            List<Document> mutableDocuments = new ArrayList<>(documents);
            Collections.shuffle(mutableDocuments, dayRandom);

            appendDocumentsToContext(contextBuilder, mutableDocuments, "Search Query: " + query);
        }

        // Search using constitutional chapters
        for (String chapter : targetChapters) {
            List<Document> chapterDocs = searchFunction.search(chapter, 2, threshold);
            appendDocumentsToContext(contextBuilder, chapterDocs, "Constitutional Reference: " + chapter);
        }

        String context = contextBuilder.toString();
        log.debug("Built diverse context with {} characters using {} queries and {} chapters",
                context.length(), searchQueries.size(), targetChapters.size());

        return context;
    }

    /**
     * Append documents to context with proper formatting
     */
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

    /**
     * Calculate difficulty distribution for questions
     */
    public String calculateDifficultyDistribution(int questionCount) {
        int easy = questionCount / 3;
        int medium = questionCount / 3;
        int hard = questionCount - (easy + medium);
        return String.format("%d Easy, %d Medium, %d Hard", easy, medium, hard);
    }

    /**
     * Build prompt variables for AI generation
     */
    public Map<String, Object> buildPromptVariables(String context, LocalDate quizDate,
                                                    int questionCount, String focusAreas,
                                                    List<String> targetChapters) {
        return Map.of(
                "context", context != null ? context : "",
                "quizDate", quizDate.toString(),
                "questionCount", String.valueOf(questionCount),
                "difficultyDistribution", calculateDifficultyDistribution(questionCount),
                "focusAreas", focusAreas,
                "targetChapters", String.join(", ", targetChapters)
        );
    }

    /**
     * Clean JSON response from AI
     */
    public String cleanJsonResponse(String response) {
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

    /**
     * Get quiz diversity information for a specific date
     */
    public Map<String, Object> getQuizDiversityInfo(LocalDate date, QuizConfigurationProperties quizConfig) {
        long epochDay = date.toEpochDay();
        QuizConfigurationProperties.SearchStrategy strategy = quizConfig.getSearchStrategyForDate(epochDay);
        List<String> targetChapters = selectTargetChapters(date, quizConfig.getConstitutionalChapters());

        return Map.of(
                "date", date.toString(),
                "strategyName", strategy.getName(),
                "focusAreas", strategy.getName(),
                "targetChapters", targetChapters,
                "searchQueries", strategy.getQueries(),
                "totalStrategies", quizConfig.getDailySearchStrategies().size()
        );
    }

    /**
     * Validate quiz generation response
     */
    public void validateQuizResponse(Object response, int expectedQuestionCount, String operation) {
        if (response == null) {
            throw new IllegalArgumentException("No response generated for " + operation);
        }

        log.info("Quiz validation passed for {} operation", operation);
    }

    /**
     * Create default metadata for document processing
     */
    public Map<String, String> createDefaultMetadata(String storagePath) {
        Map<String, String> metadata = new HashMap<>();

        String filename = storagePath.substring(storagePath.lastIndexOf('/') + 1);
        metadata.put("filename", filename);
        metadata.put("upload_source", "ai_processing");

        if (filename.toLowerCase().contains("constitution")) {
            metadata.put("primary_category", "Constitutional Law");
        } else if (filename.toLowerCase().contains("bill") || filename.toLowerCase().contains("law")) {
            metadata.put("primary_category", "Legislation");
        } else if (filename.toLowerCase().contains("budget") || filename.toLowerCase().contains("finance")) {
            metadata.put("primary_category", "Public Finance");
        } else if (filename.toLowerCase().contains("elections") || filename.toLowerCase().contains("acts")) {
            metadata.put("primary_category", "Elections Act");
        }
        else {
            metadata.put("primary_category", "General Civic Education");
        }

        return metadata;
    }

    @FunctionalInterface
    public interface ContextSearchFunction {
        List<Document> search(String query, int limit, double threshold);
    }
}
