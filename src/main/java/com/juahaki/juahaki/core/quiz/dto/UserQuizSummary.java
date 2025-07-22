package com.juahaki.juahaki.core.quiz.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class UserQuizSummary {
    private String sessionId;
    private Long attemptId;
    private String quizTitle;
    private int totalQuestions;
    private int questionsAnswered;
    private int correctAnswers;
    private int score;
    private String performanceLevel;
    private LocalDateTime startedAt;
    private LocalDateTime completedAt;
    private Long durationSeconds;
    private String durationFormatted;
    private List<QuestionResultSummary> questionResults;
    private CategoryPerformance categoryPerformance;
    private String completionMessage;
}
