package com.juahaki.juahaki.core.quiz.dto;

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
