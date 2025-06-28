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
public class CivicQuestionResponse {
    private Long questionId;
    private int questionNumber;
    private String questionText;
    private String category;
    private String difficulty;
    private List<QuestionOptionResponse> options;
    private String sourceReference;

    private String correctAnswer;
    private String explanation;
    private String correctOptionText;

    public CivicQuestionResponse forUser() {
        return CivicQuestionResponse.builder()
                .questionId(this.questionId)
                .questionNumber(this.questionNumber)
                .questionText(this.questionText)
                .category(this.category)
                .difficulty(this.difficulty)
                .options(this.options)
                .sourceReference(this.sourceReference)
                .build();
    }
}
