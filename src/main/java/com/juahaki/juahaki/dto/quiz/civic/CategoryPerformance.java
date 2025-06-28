package com.juahaki.juahaki.dto.quiz.civic;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class CategoryPerformance {
    private Map<String, CategoryStats> categoryStats;
    private String strongestCategory;
    private String weakestCategory;
    private String overallFeedback;
}