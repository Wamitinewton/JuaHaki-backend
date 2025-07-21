package com.juahaki.juahaki.core.quiz.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class UserQuizMetadata {
    private String sessionId;
    private Long attemptId;
    private String quizTitle;
    private LocalDateTime quizDate;
    private int totalQuestions;
    private int questionsAnswered;
    private int correctAnswers;
    private int score;
    private String performanceLevel;
    private LocalDateTime startedAt;
    private LocalDateTime completedAt;
    private String durationFormatted;
    private String completionMessage;
    private boolean isCompleted;
}