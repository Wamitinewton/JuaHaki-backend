package com.juahaki.juahaki.dto.quiz.civic;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class SubmitCivicAnswerResponse {
    private boolean correct;
    private String message;
    private String correctAnswer;
    private String correctOptionText;
    private String explanation;
    private int currentScore;
    private int questionsAnswered;
    private int totalQuestions;
    private boolean hasNextQuestion;
    private CivicQuestionResponse nextQuestion;
    private UserQuizSummary finalResults;
    private boolean successful;
    private String errorMessage;

    public static SubmitCivicAnswerResponse success(boolean correct, String message,
                                                    String correctAnswer, String correctOptionText,
                                                    String explanation, int currentScore,
                                                    int questionsAnswered, int totalQuestions,
                                                    boolean hasNextQuestion,
                                                    CivicQuestionResponse nextQuestion,
                                                    UserQuizSummary finalResults) {
        return SubmitCivicAnswerResponse.builder()
                .correct(correct)
                .message(message)
                .correctAnswer(correctAnswer)
                .correctOptionText(correctOptionText)
                .explanation(explanation)
                .currentScore(currentScore)
                .questionsAnswered(questionsAnswered)
                .totalQuestions(totalQuestions)
                .hasNextQuestion(hasNextQuestion)
                .nextQuestion(nextQuestion)
                .finalResults(finalResults)
                .successful(true)
                .build();
    }

    public static SubmitCivicAnswerResponse error(String errorMessage) {
        return SubmitCivicAnswerResponse.builder()
                .successful(false)
                .errorMessage(errorMessage)
                .build();
    }

    public SubmitCivicAnswerResponse forUser() {
        CivicQuestionResponse userNextQuestion = null;
        if (nextQuestion != null) {
            userNextQuestion = nextQuestion.forUser();
        }

        return SubmitCivicAnswerResponse.builder()
                .correct(this.correct)
                .message(this.message)
                .correctAnswer(this.correctAnswer)
                .correctOptionText(this.correctOptionText)
                .explanation(this.explanation)
                .currentScore(this.currentScore)
                .questionsAnswered(this.questionsAnswered)
                .totalQuestions(this.totalQuestions)
                .hasNextQuestion(this.hasNextQuestion)
                .nextQuestion(userNextQuestion)
                .finalResults(this.finalResults)
                .successful(this.successful)
                .errorMessage(this.errorMessage)
                .build();
    }
}