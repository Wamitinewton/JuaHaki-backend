package com.juahaki.juahaki.core.quiz.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class QuestionResultSummary {
    private int questionNumber;
    private String questionText;
    private String category;
    private String selectedAnswer;
    private String correctAnswer;
    private String selectedOptionText;
    private String correctOptionText;
    private boolean isCorrect;
    private String explanation;
    private Long timeSpentSeconds;
}
