package com.juahaki.juahaki.dto.quiz.civic;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class CivicQuizInfoResponse {
    private Long quizId;
    private LocalDate quizDate;
    private String title;
    private String description;
    private int totalQuestions;
    private boolean isActive;
    private boolean isExpired;
    private LocalDateTime expiresAt;
    private boolean hasUserAttempted;
    private UserQuizSummary userLastAttempt;
    private String timeRemaining;
}
