package com.juahaki.juahaki.dto.quiz.civic;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;


@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class QuestionDto {
    private Long questionId;
    private int questionNumber;
    private String questionText;
    private String explanation;
    private String category;
    private String difficulty;
    private String correctAnswer;
    private String sourceReference;
    private List<OptionDto> options;

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class OptionDto {
        private String optionLetter;
        private String optionText;
    }
}