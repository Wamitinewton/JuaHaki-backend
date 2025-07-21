package com.juahaki.juahaki.core.quiz.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class SubmitCivicAnswerRequest {
    @NotBlank(message = "Session ID is required")
    private String sessionId;

    @NotBlank(message = "Answer is required")
    @Pattern(regexp = "^[ABCD]$", message = "Answer must be A, B, C, or D")
    private String answer;

    private Long timeSpentSeconds;
}
