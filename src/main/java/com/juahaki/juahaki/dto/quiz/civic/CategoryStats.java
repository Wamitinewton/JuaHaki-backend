package com.juahaki.juahaki.dto.quiz.civic;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class CategoryStats {
    private String category;
    private int totalQuestions;
    private int correctAnswers;
    private double percentage;
    private String performance;
    private String feedback;
}
