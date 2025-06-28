package com.juahaki.juahaki.dto.quiz.civic;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class StartCivicQuizResponse {
    private String sessionId;
    private Long quizId;
    private String title;
    private int totalQuestions;
    private CivicQuestionResponse currentQuestion;
    private boolean successful;
    private String errorMessage;

    public static StartCivicQuizResponse success(String sessionId, Long quizId, String title,
                                                 int totalQuestions, CivicQuestionResponse currentQuestion) {
        return StartCivicQuizResponse.builder()
                .sessionId(sessionId)
                .quizId(quizId)
                .title(title)
                .totalQuestions(totalQuestions)
                .currentQuestion(currentQuestion)
                .successful(true)
                .build();
    }

    public static StartCivicQuizResponse error(String errorMessage) {
        return StartCivicQuizResponse.builder()
                .successful(false)
                .errorMessage(errorMessage)
                .build();
    }
}
