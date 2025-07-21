package com.juahaki.juahaki.infrastructure.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@ConfigurationProperties(prefix = "quiz")
@Data
public class QuizConfigurationProperties {

    private List<SearchStrategy> dailySearchStrategies;
    private List<String> constitutionalChapters;
    private Generation generation = new Generation();

    public SearchStrategy getSearchStrategyForDate(long epochDay) {
        int strategyIndex = (int) (epochDay % dailySearchStrategies.size());
        return dailySearchStrategies.get(strategyIndex);
    }

    public String getFocusAreasForDate(long epochDay) {
        SearchStrategy strategy = getSearchStrategyForDate(epochDay);
        return strategy.getName();
    }

    public List<String> getSearchQueriesForDate(long epochDay) {
        SearchStrategy strategy = getSearchStrategyForDate(epochDay);
        return strategy.getQueries();
    }

    @Data
    public static class SearchStrategy {
        private String name;
        private List<String> queries;
    }

    @Data
    public static class Generation {
        private int maxContextDocuments = 15;
        private double similarityThreshold = 0.6;
        private int defaultQuestionCount = 10;
    }
}
