package com.juahaki.juahaki.dto.quiz.civic;

import com.juahaki.juahaki.enums.QuizStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class CivicQuizSessionResponse {
    private String sessionId;
    private Long attemptId;
    private QuizStatus status;
    private String quizTitle;
    private int totalQuestions;
    private int questionsAnswered;
    private int currentScore;
    private CivicQuestionResponse currentQuestion;
    private LocalDateTime startedAt;
    private boolean isExpired;
    private String timeRemaining;
    private boolean successful;
    private String errorMessage;

    public static CivicQuizSessionResponse success(String sessionId, Long attemptId,
                                                   QuizStatus status, String quizTitle,
                                                   int totalQuestions, int questionsAnswered,
                                                   int currentScore, CivicQuestionResponse currentQuestion,
                                                   LocalDateTime startedAt, boolean isExpired,
                                                   String timeRemaining) {
        return CivicQuizSessionResponse.builder()
                .sessionId(sessionId)
                .attemptId(attemptId)
                .status(status)
                .quizTitle(quizTitle)
                .totalQuestions(totalQuestions)
                .questionsAnswered(questionsAnswered)
                .currentScore(currentScore)
                .currentQuestion(currentQuestion)
                .startedAt(startedAt)
                .isExpired(isExpired)
                .timeRemaining(timeRemaining)
                .successful(true)
                .build();
    }

    public static CivicQuizSessionResponse error(String errorMessage) {
        return CivicQuizSessionResponse.builder()
                .successful(false)
                .errorMessage(errorMessage)
                .build();
    }
}

