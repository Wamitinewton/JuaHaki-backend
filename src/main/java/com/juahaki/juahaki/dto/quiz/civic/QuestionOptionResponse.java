package com.juahaki.juahaki.dto.quiz.civic;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class QuestionOptionResponse {
    private String optionLetter;
    private String optionText;
}
