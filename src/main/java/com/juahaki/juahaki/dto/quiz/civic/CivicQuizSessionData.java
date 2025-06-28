package com.juahaki.juahaki.dto.quiz.civic;

import lombok.*;


@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class CivicQuizSessionData {
    private Long userId;
    private Long quizId;
    private int currentQuestionNumber;
    private Long attemptId;
}
