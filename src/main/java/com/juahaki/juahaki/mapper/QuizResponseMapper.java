package com.juahaki.juahaki.mapper;

import com.juahaki.juahaki.dto.quiz.civic.*;
import com.juahaki.juahaki.model.quiz.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
@Slf4j
public class QuizResponseMapper {

    public CivicQuestionResponse buildQuestionResponseForUser(CivicQuestion question) {
        List<QuestionOptionResponse> options = question.getOptions().stream()
                .map(option -> QuestionOptionResponse.builder()
                        .optionLetter(option.getOptionLetter())
                        .optionText(option.getOptionText())
                        .build())
                .collect(Collectors.toList());

        return CivicQuestionResponse.builder()
                .questionId(question.getId())
                .questionNumber(question.getQuestionNumber())
                .questionText(question.getQuestionText())
                .category(question.getCategory())
                .difficulty(question.getDifficulty())
                .options(options)
                .sourceReference(question.getSourceReference())
                .build();
    }

    /**
     * Convert UserQuizAttempt to comprehensive summary
     */
    public UserQuizSummary buildUserQuizSummary(UserQuizAttempt attempt, List<UserAnswer> answers) {
        List<QuestionResultSummary> questionResults = answers.stream()
                .map(this::buildQuestionResultSummary)
                .collect(Collectors.toList());

        CategoryPerformance categoryPerformance = buildCategoryPerformance(answers);
        String durationFormatted = attempt.getDurationSeconds() != null ?
                formatDuration(Duration.ofSeconds(attempt.getDurationSeconds())) : "N/A";

        return UserQuizSummary.builder()
                .sessionId(attempt.getSessionId())
                .attemptId(attempt.getId())
                .quizTitle(attempt.getDailyQuiz().getTitle())
                .totalQuestions(attempt.getTotalQuestions())
                .questionsAnswered(attempt.getQuestionsAnswered())
                .correctAnswers(attempt.getCorrectAnswers())
                .score(attempt.getScore())
                .performanceLevel(attempt.getPerformanceLevel())
                .startedAt(attempt.getStartedAt())
                .completedAt(attempt.getCompletedAt())
                .durationSeconds(attempt.getDurationSeconds())
                .durationFormatted(durationFormatted)
                .questionResults(questionResults)
                .categoryPerformance(categoryPerformance)
                .completionMessage(generateCompletionMessage(attempt))
                .build();
    }

    /**
     * Convert UserAnswer to question result summary
     */
    public QuestionResultSummary buildQuestionResultSummary(UserAnswer answer) {
        CivicQuestion question = answer.getQuestion();

        return QuestionResultSummary.builder()
                .questionNumber(question.getQuestionNumber())
                .questionText(question.getQuestionText())
                .category(question.getCategory())
                .selectedAnswer(answer.getSelectedAnswer())
                .correctAnswer(question.getCorrectAnswer())
                .selectedOptionText(getOptionText(question, answer.getSelectedAnswer()))
                .correctOptionText(question.getCorrectOptionText())
                .isCorrect(answer.isCorrect())
                .explanation(question.getExplanation())
                .timeSpentSeconds(answer.getTimeSpentSeconds())
                .build();
    }

    /**
     * Build category performance analysis
     */
    public CategoryPerformance buildCategoryPerformance(List<UserAnswer> answers) {
        Map<String, List<UserAnswer>> answersByCategory = answers.stream()
                .collect(Collectors.groupingBy(answer -> answer.getQuestion().getCategory()));

        Map<String, CategoryStats> categoryStats = new HashMap<>();
        String strongestCategory = null;
        String weakestCategory = null;
        double bestPercentage = 0;
        double worstPercentage = 100;

        for (Map.Entry<String, List<UserAnswer>> entry : answersByCategory.entrySet()) {
            String category = entry.getKey();
            List<UserAnswer> categoryAnswers = entry.getValue();

            int total = categoryAnswers.size();
            int correct = categoryAnswers.stream().mapToInt(a -> a.isCorrect() ? 1 : 0).sum();
            double percentage = total > 0 ? (correct * 100.0 / total) : 0;

            String performance = getPerformanceLevel(percentage);
            String feedback = generateCategoryFeedback(category, percentage);

            CategoryStats stats = CategoryStats.builder()
                    .category(category)
                    .totalQuestions(total)
                    .correctAnswers(correct)
                    .percentage(percentage)
                    .performance(performance)
                    .feedback(feedback)
                    .build();

            categoryStats.put(category, stats);

            if (percentage > bestPercentage) {
                bestPercentage = percentage;
                strongestCategory = category;
            }
            if (percentage < worstPercentage) {
                worstPercentage = percentage;
                weakestCategory = category;
            }
        }

        String overallFeedback = generateOverallFeedback(categoryStats);

        return CategoryPerformance.builder()
                .categoryStats(categoryStats)
                .strongestCategory(strongestCategory)
                .weakestCategory(weakestCategory)
                .overallFeedback(overallFeedback)
                .build();
    }

    /**
     * Build leaderboard entry
     */
    public LeaderboardEntry buildLeaderboardEntry(UserQuizAttempt attempt, int rank, Long currentUserId) {
        String durationFormatted = attempt.getDurationSeconds() != null ?
                formatDuration(Duration.ofSeconds(attempt.getDurationSeconds())) : "N/A";

        return LeaderboardEntry.builder()
                .rank(rank)
                .username(attempt.getUser().getUsername())
                .firstName(attempt.getUser().getFirstName())
                .score(attempt.getScore())
                .performanceLevel(attempt.getPerformanceLevel())
                .completedAt(attempt.getCompletedAt())
                .durationSeconds(attempt.getDurationSeconds())
                .durationFormatted(durationFormatted)
                .isCurrentUser(attempt.getUser().getId().equals(currentUserId))
                .build();
    }

    /**
     * Build quiz statistics
     */
    public QuizStatistics buildQuizStatistics(DailyQuiz quiz, long totalAttempts,
                                              long completedAttempts, Double averageScore) {
        double completionRate = totalAttempts > 0 ? (completedAttempts * 100.0 / totalAttempts) : 0;

        return QuizStatistics.builder()
                .totalAttempts((int) totalAttempts)
                .completedAttempts((int) completedAttempts)
                .averageScore(averageScore != null ? averageScore : 0.0)
                .completionRate(completionRate)
                .mostDifficultQuestion("Analysis pending")
                .easiestQuestion("Analysis pending")
                .popularCategory("General Knowledge")
                .build();
    }

    /**
     * Build quiz info response
     */
    public CivicQuizInfoResponse buildQuizInfoResponse(DailyQuiz quiz, boolean hasUserAttempted,
                                                       UserQuizSummary lastAttempt) {
        return CivicQuizInfoResponse.builder()
                .quizId(quiz.getId())
                .quizDate(quiz.getQuizDate())
                .title(quiz.getTitle())
                .description(quiz.getDescription())
                .totalQuestions(quiz.getTotalQuestions())
                .isActive(quiz.isActive())
                .isExpired(quiz.isExpired())
                .expiresAt(quiz.getExpiresAt())
                .hasUserAttempted(hasUserAttempted)
                .userLastAttempt(lastAttempt)
                .timeRemaining(calculateTimeRemaining(quiz.getExpiresAt()))
                .build();
    }

    private String getOptionText(CivicQuestion question, String optionLetter) {
        return question.getOptions().stream()
                .filter(option -> option.getOptionLetter().equals(optionLetter))
                .findFirst()
                .map(QuestionOption::getOptionText)
                .orElse("");
    }

    private String getPerformanceLevel(double percentage) {
        if (percentage >= 80) return "Excellent";
        if (percentage >= 70) return "Good";
        if (percentage >= 60) return "Fair";
        return "Needs Improvement";
    }

    private String generateCategoryFeedback(String category, double percentage) {
        if (percentage >= 80) {
            return "Outstanding knowledge in " + category + "! Keep up the excellent work";
        } else if (percentage >= 70) {
            return "Good understanding of " + category + ". A few more correct answers would make it excellent.";
        } else if (percentage >= 60) {
            return "Fair knowledge of " + category + ". Consider reviewing this topic more thoroughly.";
        } else {
            return "This " + category + " area needs attention. Focus on studying this topic more.";
        }
    }

    private String generateOverallFeedback(Map<String, CategoryStats> categoryStats) {
        double averagePercentage = categoryStats.values().stream()
                .mapToDouble(CategoryStats::getPercentage)
                .average()
                .orElse(0);

        if (averagePercentage >= 80) {
            return "Excellent overall performance! You have a strong grasp of civic knowledge.";
        } else if (averagePercentage >= 70) {
            return "Good overall performance. You're on the right track with your civic education.";
        } else if (averagePercentage >= 60) {
            return "Fair performance. Continue studying to improve your civic knowledge.";
        } else {
            return "Keep practicing! Regular study will help improve your civic understanding.";
        }
    }

    private String generateCompletionMessage(UserQuizAttempt attempt) {
        int score = attempt.getScore();
        if (score >= 90) {
            return "Outstanding! You're a civic knowledge champion! 🏆";
        } else if (score >= 80) {
            return "Excellent work! You have strong civic knowledge! 🌟";
        } else if (score >= 70) {
            return "Good job! You're well-informed about civic matters! 👍";
        } else if (score >= 60) {
            return "Fair performance. Keep learning to improve! 📚";
        } else {
            return "Keep studying! Every quiz makes you more informed! 💪";
        }
    }

    private String calculateTimeRemaining(LocalDateTime expiresAt) {
        if (expiresAt == null) return "Unknown";

        Duration remaining = Duration.between(LocalDateTime.now(), expiresAt);
        if (remaining.isNegative()) {
            return "Expired";
        }

        return formatDuration(remaining);
    }

    private String formatDuration(Duration duration) {
        long hours = duration.toHours();
        long minutes = duration.toMinutesPart();
        long seconds = duration.toSecondsPart();

        if (hours > 0) {
            return String.format("%dh %dm", hours, minutes);
        } else if (minutes > 0) {
            return String.format("%dm %ds", minutes, seconds);
        } else {
            return String.format("%ds", seconds);
        }
    }
}
