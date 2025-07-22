package com.juahaki.juahaki.core.quiz.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class QuizStatistics {
    private int totalAttempts;
    private int completedAttempts;
    private double averageScore;
    private double completionRate;
    private String mostDifficultQuestion;
    private String easiestQuestion;
    private String popularCategory;
}
