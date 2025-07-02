package com.juahaki.juahaki.util.quiz;

import com.juahaki.juahaki.dto.quiz.civic.*;
import com.juahaki.juahaki.enums.QuizStatus;
import com.juahaki.juahaki.model.quiz.DailyQuiz;
import com.juahaki.juahaki.model.quiz.UserQuizAttempt;
import lombok.experimental.UtilityClass;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@UtilityClass
public class QuizSessionBuilder {

    /**
     * Generate unique session ID for civic quiz
     */
    public String generateSessionId() {
        return "civic_" + UUID.randomUUID().toString().replace("-", "").substring(0, 16);
    }

    /**
     * Create session data for Redis storage
     */
    public CivicQuizSessionData createSessionData(Long userId, Long quizId, Long attemptId) {
        return new CivicQuizSessionData(userId, quizId, 1, attemptId);
    }

    /**
     * Build successful start quiz response
     */
    public StartCivicQuizResponse buildStartQuizSuccess(String sessionId, DailyQuiz quiz,
                                                        CivicQuestionResponse firstQuestion) {
        return StartCivicQuizResponse.success(
                sessionId,
                quiz.getId(),
                quiz.getTitle(),
                quiz.getTotalQuestions(),
                firstQuestion
        );
    }


    public StartCivicQuizResponse buildStartQuizError(String errorMessage) {
        return StartCivicQuizResponse.error(errorMessage);
    }


    public CivicQuizSessionResponse buildSessionStatusSuccess(String sessionId, UserQuizAttempt attempt,
                                                              CivicQuestionResponse currentQuestion,
                                                              String timeRemaining) {
        return CivicQuizSessionResponse.success(
                sessionId,
                attempt.getId(),
                attempt.getStatus(),
                attempt.getDailyQuiz().getTitle(),
                attempt.getTotalQuestions(),
                attempt.getQuestionsAnswered(),
                attempt.getScore(),
                currentQuestion,
                attempt.getStartedAt(),
                attempt.isExpired(),
                timeRemaining
        );
    }


    public CivicQuizSessionResponse buildSessionStatusError(String errorMessage) {
        return CivicQuizSessionResponse.error(errorMessage);
    }


    public SubmitCivicAnswerResponse buildSubmitAnswerSuccess(boolean isCorrect, String correctAnswer,
                                                              String correctOptionText, String explanation,
                                                              UserQuizAttempt attempt, boolean hasNextQuestion,
                                                              CivicQuestionResponse nextQuestion,
                                                              UserQuizSummary finalResults) {
        String message = isCorrect ? "Correct answer!" : "Incorrect answer.";

        return SubmitCivicAnswerResponse.success(
                isCorrect,
                message,
                correctAnswer,
                correctOptionText,
                explanation,
                attempt.getScore(),
                attempt.getQuestionsAnswered(),
                attempt.getTotalQuestions(),
                hasNextQuestion,
                nextQuestion,
                finalResults
        );
    }


    public SubmitCivicAnswerResponse buildSubmitAnswerError(String errorMessage) {
        return SubmitCivicAnswerResponse.error(errorMessage);
    }


    public QuizLeaderboardResponse buildLeaderboardResponse(DailyQuiz quiz,
                                                            List<LeaderboardEntry> topPerformers,
                                                            LeaderboardEntry userRanking,
                                                            QuizStatistics statistics,
                                                            long totalParticipants) {
        return QuizLeaderboardResponse.builder()
                .quizDate(quiz.getQuizDate())
                .quizTitle(quiz.getTitle())
                .totalParticipants((int) totalParticipants)
                .topPerformers(topPerformers)
                .userRanking(userRanking)
                .statistics(statistics)
                .build();
    }

    /**
     * Check if quiz session has expired
     */
    public boolean isSessionExpired(UserQuizAttempt attempt, long timeoutMinutes) {
        return attempt.getStatus() == QuizStatus.ACTIVE &&
                LocalDateTime.now().isAfter(attempt.getStartedAt().plusMinutes(timeoutMinutes));
    }

    /**
     * Update session data for next question
     */
    public void moveToNextQuestion(CivicQuizSessionData sessionData) {
        sessionData.setCurrentQuestionNumber(sessionData.getCurrentQuestionNumber() + 1);
    }

    /**
     * Check if quiz is complete
     */
    public boolean isQuizComplete(CivicQuizSessionData sessionData, int totalQuestions) {
        return sessionData.getCurrentQuestionNumber() > totalQuestions;
    }
}