package com.juahaki.juahaki.shared.utils.quiz;

import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;

import java.time.LocalDate;
import java.util.*;

@UtilityClass
@Slf4j
public class QuizAIBuilder {


    public List<String> selectTargetChapters(LocalDate quizDate, List<String> constitutionalChapters) {
        List<String> shuffledChapters = new ArrayList<>(constitutionalChapters);
        Collections.shuffle(shuffledChapters, new Random(quizDate.toEpochDay()));
        return shuffledChapters.subList(0, Math.min(4, shuffledChapters.size()));
    }


    public String buildDiverseContext(List<String> searchQueries, List<String> targetChapters,
                                      LocalDate quizDate, int maxDocs, double threshold,
                                      ContextSearchFunction searchFunction) {
        StringBuilder contextBuilder = new StringBuilder();

        for (String query : searchQueries) {
            List<Document> documents = searchFunction.search(query, maxDocs / searchQueries.size(), threshold);
            appendDocumentsToContext(contextBuilder, documents, query);
        }

        for (String chapter : targetChapters) {
            List<Document> chapterDocs = searchFunction.search(chapter, 2, threshold);
            appendDocumentsToContext(contextBuilder, chapterDocs, chapter);
        }

        String context = contextBuilder.toString();
        log.debug("Built context with {} characters", context.length());
        return context;
    }


    private void appendDocumentsToContext(StringBuilder contextBuilder, List<Document> documents, String source) {
        for (Document doc : documents) {
            contextBuilder.append("Source: ").append(source)
                    .append(" | Page: ").append(doc.getMetadata().getOrDefault("page_number", "N/A"))
                    .append("\n")
                    .append(doc.getText())
                    .append("\n\n");
        }
    }


    public String calculateDifficultyDistribution(int questionCount) {
        int easy = questionCount / 3;
        int medium = questionCount / 3;
        int hard = questionCount - (easy + medium);
        return String.format("%d Easy, %d Medium, %d Hard", easy, medium, hard);
    }


    public Map<String, Object> buildPromptVariables(String context, LocalDate quizDate,
                                                    int questionCount, String focusAreas,
                                                    List<String> targetChapters) {
        return Map.of(
                "context", context != null ? context : "",
                "quizDate", quizDate.toString(),
                "questionCount", String.valueOf(questionCount),
                "difficultyDistribution", calculateDifficultyDistribution(questionCount),
                "focusAreas", focusAreas
        );
    }


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


    public void validateQuizResponse(Object response, int expectedQuestionCount, String operation) {
        if (response == null) {
            throw new IllegalArgumentException("No response generated for " + operation);
        }
        log.info("Quiz validation passed for {} operation", operation);
    }

    @FunctionalInterface
    public interface ContextSearchFunction {
        List<Document> search(String query, int limit, double threshold);
    }
}