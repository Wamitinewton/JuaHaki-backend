package com.juahaki.juahaki.service.quiz;

import com.juahaki.juahaki.dto.quiz.civic.*;
import com.juahaki.juahaki.enums.QuizStatus;
import com.juahaki.juahaki.exception.CustomException;
import com.juahaki.juahaki.model.quiz.*;
import com.juahaki.juahaki.model.user.User;
import com.juahaki.juahaki.repository.quiz.CivicQuestionRepository;
import com.juahaki.juahaki.repository.quiz.DailyQuizRepository;
import com.juahaki.juahaki.repository.quiz.UserAnswerRepository;
import com.juahaki.juahaki.repository.quiz.UserQuizAttemptRepository;
import com.juahaki.juahaki.repository.user.UserRepository;
import com.juahaki.juahaki.util.jwt.JwtHelperService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class CivicQuizService implements ICivicQuizService {

    private final DailyQuizRepository dailyQuizRepository;
    private final CivicQuestionRepository civicQuestionRepository;
    private final UserQuizAttemptRepository userQuizAttemptRepository;
    private final UserAnswerRepository userAnswerRepository;
    private final UserRepository userRepository;
    private final JwtHelperService jwtHelperService;
    private final IQuizRedisService quizRedisService;

    private static final long SESSION_TIMEOUT_MINUTES = 30;

    @Override
    @Cacheable(value = "quizInfo", key = "'today_' + #request.getHeader('Authorization')")
    public CivicQuizInfoResponse getTodaysInfo(HttpServletRequest request) {
        log.debug("Getting today's quiz info");
        return getQuizInfo(LocalDate.now(), request);
    }

    @Override
    @Cacheable(value = "quizInfo", key = "#date + '_' + #request.getHeader('Authorization')")
    public CivicQuizInfoResponse getQuizInfo(LocalDate date, HttpServletRequest request) {
        log.debug("Getting quiz info for date: {}", date);

        Long userId = jwtHelperService.getCurrentUserIdFromRequest(request);

        Optional<DailyQuiz> quizOptional = dailyQuizRepository.findActiveQuizByDate(date);
        if (quizOptional.isEmpty()) {
            throw new CustomException("No quiz available for the selected date");
        }

        DailyQuiz quiz = quizOptional.get();
        Optional<UserQuizAttempt> userAttempt = userQuizAttemptRepository.findByUserAndQuizDate(
                userRepository.findById(userId).orElseThrow(), date);

        UserQuizSummary lastAttempt = null;
        if (userAttempt.isPresent()) {
            lastAttempt = buildUserQuizSummary(userAttempt.get());
        }

        return CivicQuizInfoResponse.builder()
                .quizId(quiz.getId())
                .quizDate(quiz.getQuizDate())
                .title(quiz.getTitle())
                .description(quiz.getDescription())
                .totalQuestions(quiz.getTotalQuestions())
                .isActive(quiz.isActive())
                .isExpired(quiz.isExpired())
                .expiresAt(quiz.getExpiresAt())
                .hasUserAttempted(userAttempt.isPresent())
                .userLastAttempt(lastAttempt)
                .timeRemaining(calculateTimeRemaining(quiz.getExpiresAt()))
                .build();
    }

    @Override
    @Transactional
    @CacheEvict(value = {"quizInfo", "leaderboard", "quizStats"}, key = "#request.getHeader('Authorization')")
    public StartCivicQuizResponse startQuiz(HttpServletRequest request) {
        log.debug("Starting civic quiz");

        Long userId = jwtHelperService.getCurrentUserIdFromRequest(request);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException("User not found"));

        DailyQuiz todaysQuiz = dailyQuizRepository.findTodaysActiveQuiz()
                .orElseThrow(() -> new CustomException("No active quiz available today"));

        if (todaysQuiz.isExpired()) {
            throw new CustomException("Today's quiz has expired");
        }

        // Check if user already has an active session
        if (userQuizAttemptRepository.existsByUserAndDailyQuiz(user, todaysQuiz)) {
            throw new CustomException("You have already attempted today's quiz");
        }

        try {
            // Create new quiz attempt
            String sessionId = generateSessionId();
            UserQuizAttempt attempt = createQuizAttempt(user, todaysQuiz, sessionId);
            UserQuizAttempt savedAttempt = userQuizAttemptRepository.save(attempt);

            // Store session in Redis
            CivicQuizSessionData sessionData = new CivicQuizSessionData(
                    userId, todaysQuiz.getId(), 1, savedAttempt.getId());

            quizRedisService.storeCivicQuizSession(sessionId, sessionData,
                    SESSION_TIMEOUT_MINUTES, TimeUnit.MINUTES);

            // Get first question 
            CivicQuestion firstQuestion = getQuestionByNumber(todaysQuiz, 1);

            CivicQuestionResponse questionResponse = buildQuestionResponseForUser(firstQuestion);

            log.info("Started quiz session {} for user {}", sessionId, userId);

            return StartCivicQuizResponse.success(sessionId, todaysQuiz.getId(),
                    todaysQuiz.getTitle(), todaysQuiz.getTotalQuestions(), questionResponse);

        } catch (Exception e) {
            log.error("Error starting quiz for user {}: {}", userId, e.getMessage(), e);
            return StartCivicQuizResponse.error("Failed to start quiz: " + e.getMessage());
        }
    }

    @Override
    @Transactional
    @CacheEvict(value = {"leaderboard", "quizStats", "userQuizHistory"}, allEntries = true)
    public SubmitCivicAnswerResponse submitAnswer(SubmitCivicAnswerRequest request,
                                                  HttpServletRequest httpRequest) {
        log.debug("Submitting answer for session: {}", request.getSessionId());

        Long userId = jwtHelperService.getCurrentUserIdFromRequest(httpRequest);

        // Get session data from Redis
        Optional<CivicQuizSessionData> sessionOptional =
                quizRedisService.getCivicQuizSession(request.getSessionId());

        if (sessionOptional.isEmpty()) {
            return SubmitCivicAnswerResponse.error("Invalid or expired session");
        }

        CivicQuizSessionData sessionData = sessionOptional.get();

        if (!sessionData.getUserId().equals(userId)) {
            return SubmitCivicAnswerResponse.error("Session does not belong to current user");
        }

        try {
            UserQuizAttempt attempt = userQuizAttemptRepository.findById(sessionData.getAttemptId())
                    .orElseThrow(() -> new CustomException("Quiz attempt not found"));

            if (attempt.getStatus() != QuizStatus.ACTIVE) {
                return SubmitCivicAnswerResponse.error("Quiz session is not active");
            }

            // Get current question 
            CivicQuestion currentQuestion = getQuestionByNumber(attempt.getDailyQuiz(), sessionData.getCurrentQuestionNumber());

            // Check if already answered
            if (userAnswerRepository.existsByAttemptAndQuestion(attempt, currentQuestion)) {
                return SubmitCivicAnswerResponse.error("Question already answered");
            }

            // Process the answer
            boolean isCorrect = currentQuestion.getCorrectAnswer().equals(request.getAnswer());

            // Save user answer
            UserAnswer userAnswer = UserAnswer.builder()
                    .attempt(attempt)
                    .question(currentQuestion)
                    .selectedAnswer(request.getAnswer())
                    .isCorrect(isCorrect)
                    .timeSpentSeconds(request.getTimeSpentSeconds())
                    .build();
            userAnswerRepository.save(userAnswer);

            // Update attempt statistics
            attempt.setQuestionsAnswered(attempt.getQuestionsAnswered() + 1);
            if (isCorrect) {
                attempt.setCorrectAnswers(attempt.getCorrectAnswers() + 1);
            }

            boolean hasNextQuestion = sessionData.getCurrentQuestionNumber() < attempt.getTotalQuestions();
            CivicQuestionResponse nextQuestion = null;
            UserQuizSummary finalResults = null;

            if (hasNextQuestion) {
                // Move to next question
                sessionData.setCurrentQuestionNumber(sessionData.getCurrentQuestionNumber() + 1);
                quizRedisService.updateCivicQuizSession(request.getSessionId(), sessionData,
                        SESSION_TIMEOUT_MINUTES, TimeUnit.MINUTES);

                CivicQuestion next = getQuestionByNumber(attempt.getDailyQuiz(), sessionData.getCurrentQuestionNumber());

                if (next != null) {
                    nextQuestion = buildQuestionResponseForUser(next);
                }
            } else {
                // Quiz completed
                attempt.completeQuiz();
                finalResults = buildUserQuizSummary(attempt);

                // Remove session from Redis
                quizRedisService.removeCivicQuizSession(request.getSessionId());

                log.info("Quiz completed for user {} with score {}", userId, attempt.getScore());
            }

            userQuizAttemptRepository.save(attempt);

            String message = isCorrect ? "Correct answer!" : "Incorrect answer.";

            SubmitCivicAnswerResponse response = SubmitCivicAnswerResponse.success(
                    isCorrect, message, currentQuestion.getCorrectAnswer(),
                    currentQuestion.getCorrectOptionText(), currentQuestion.getExplanation(),
                    attempt.getScore(), attempt.getQuestionsAnswered(), attempt.getTotalQuestions(),
                    hasNextQuestion, nextQuestion, finalResults);

            return response.forUser();

        } catch (Exception e) {
            log.error("Error submitting answer for session {}: {}", request.getSessionId(), e.getMessage(), e);
            return SubmitCivicAnswerResponse.error("Failed to submit answer: " + e.getMessage());
        }
    }

    @Override
    public CivicQuizSessionResponse getSessionStatus(String sessionId, HttpServletRequest request) {
        log.debug("Getting session status for: {}", sessionId);

        Long userId = jwtHelperService.getCurrentUserIdFromRequest(request);

        Optional<CivicQuizSessionData> sessionOptional =
                quizRedisService.getCivicQuizSession(sessionId);

        if (sessionOptional.isEmpty()) {
            return CivicQuizSessionResponse.error("Session not found or expired");
        }

        CivicQuizSessionData sessionData = sessionOptional.get();

        if (!sessionData.getUserId().equals(userId)) {
            return CivicQuizSessionResponse.error("Session does not belong to current user");
        }

        try {
            UserQuizAttempt attempt = userQuizAttemptRepository.findById(sessionData.getAttemptId())
                    .orElseThrow(() -> new CustomException("Quiz attempt not found"));

            if (attempt.isExpired()) {
                attempt.setStatus(QuizStatus.EXPIRED);
                userQuizAttemptRepository.save(attempt);
                quizRedisService.removeCivicQuizSession(sessionId);
                return CivicQuizSessionResponse.error("Session has expired");
            }

            CivicQuestion currentQuestion = getQuestionByNumber(attempt.getDailyQuiz(), sessionData.getCurrentQuestionNumber());

            CivicQuestionResponse questionResponse = currentQuestion != null ?
                    buildQuestionResponseForUser(currentQuestion) : null;

            long ttl = quizRedisService.getSessionTTL(sessionId);
            String timeRemaining = ttl > 0 ? formatDuration(Duration.ofSeconds(ttl)) : "Expired";

            return CivicQuizSessionResponse.success(sessionId, attempt.getId(), attempt.getStatus(),
                    attempt.getDailyQuiz().getTitle(), attempt.getTotalQuestions(),
                    attempt.getQuestionsAnswered(), attempt.getScore(), questionResponse,
                    attempt.getStartedAt(), attempt.isExpired(), timeRemaining);

        } catch (Exception e) {
            log.error("Error getting session status for {}: {}", sessionId, e.getMessage(), e);
            return CivicQuizSessionResponse.error("Failed to get session status");
        }
    }

    @Override
    @Transactional
    public void abandonSession(String sessionId, HttpServletRequest request) {
        Long userId = jwtHelperService.getCurrentUserIdFromRequest(request);

        Optional<CivicQuizSessionData> sessionDataOptional =
                quizRedisService.getCivicQuizSession(sessionId);

        if (sessionDataOptional.isPresent()) {
            CivicQuizSessionData sessionData = sessionDataOptional.get();

            if (sessionData.getUserId().equals(userId)) {
                try {
                    UserQuizAttempt attempt = userQuizAttemptRepository.findById(sessionData.getAttemptId())
                            .orElse(null);
                    if (attempt != null && attempt.getStatus() == QuizStatus.ACTIVE) {
                        attempt.setStatus(QuizStatus.ABANDONED);
                        userQuizAttemptRepository.save(attempt);
                    }
                } catch (Exception e) {
                    log.error("Error updating attempt status for abandoned session: {}", e.getMessage());
                }
            }
            quizRedisService.removeCivicQuizSession(sessionId);
        }
    }

    @Override
    @Cacheable(value = "userQuizHistory", key = "'results_' + #sessionId + '_' + #request.getHeader('Authorization')")
    public UserQuizSummary getQuizResults(String sessionId, HttpServletRequest request) {

        Optional<UserQuizAttempt> attemptOptional = userQuizAttemptRepository.findBySessionId(sessionId);

        if (attemptOptional.isEmpty()) {
            throw new CustomException("Quiz results not found");
        }

        UserQuizAttempt attempt = attemptOptional.get();
        Long userId = jwtHelperService.getCurrentUserIdFromRequest(request);

        if (!attempt.getUser().getId().equals(userId)) {
            throw new CustomException("Access denied to quiz results");
        }

        if (attempt.getStatus() != QuizStatus.COMPLETED) {
            throw new CustomException("Quiz not completed yet");
        }

        return buildUserQuizSummary(attempt);
    }

    @Override
    @Cacheable(value = "userQuizHistory", key = "#request.getHeader('Authorization')")
    public List<UserQuizSummary> getUserQuizHistory(HttpServletRequest request) {
        Long userId = jwtHelperService.getCurrentUserIdFromRequest(request);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException("User not found"));

        List<UserQuizAttempt> completedAttempts = userQuizAttemptRepository
                .findCompletedAttemptsByUser(user);

        return completedAttempts.stream()
                .map(this::buildUserQuizSummary)
                .collect(Collectors.toList());
    }

    @Override
    @Cacheable(value = "leaderboard", key = "'today_' + #request.getHeader('Authorization')")
    public QuizLeaderboardResponse getTodaysLeaderboard(HttpServletRequest request) {
        return getQuizLeaderboardResponse(LocalDate.now(), request);
    }

    @Override
    @Cacheable(value = "leaderboard", key = "#date + '_' + #request.getHeader('Authorization')")
    public QuizLeaderboardResponse getQuizLeaderboardResponse(LocalDate date, HttpServletRequest request) {
        Optional<DailyQuiz> quizOptional = dailyQuizRepository.findByQuizDate(date);
        if (quizOptional.isEmpty()) {
            throw new CustomException("No quiz found for the specified date");
        }

        DailyQuiz quiz = quizOptional.get();
        Long userId = jwtHelperService.getCurrentUserIdFromRequest(request);

        List<UserQuizAttempt> topAttempts = userQuizAttemptRepository
                .findLeaderboardByQuiz(quiz).stream()
                .limit(10)
                .toList();

        List<LeaderboardEntry> topPerformers = new ArrayList<>();
        for (int i = 0; i < topAttempts.size(); i++) {
            UserQuizAttempt attempt = topAttempts.get(i);
            LeaderboardEntry entry = buildLeaderboardEntry(attempt, i + 1, userId);
            topPerformers.add(entry);
        }

        LeaderboardEntry userRanking = null;
        Optional<UserQuizAttempt> userQuizAttempt = userQuizAttemptRepository
                .findByUserAndQuizDate(userRepository.findById(userId).orElseThrow(), date);

        if (userQuizAttempt.isPresent() && userQuizAttempt.get().getStatus() == QuizStatus.COMPLETED) {
            List<UserQuizAttempt> allAttempts = userQuizAttemptRepository.findLeaderboardByQuiz(quiz);
            int userRank = allAttempts.indexOf(userQuizAttempt.get()) + 1;
            userRanking = buildLeaderboardEntry(userQuizAttempt.get(), userRank, userId);
        }

        QuizStatistics statistics = getQuizStatistics(date);
        long totalParticipants = userQuizAttemptRepository.countCompletedAttemptsByQuiz(quiz);

        return QuizLeaderboardResponse.builder()
                .quizDate(date)
                .quizTitle(quiz.getTitle())
                .totalParticipants((int) totalParticipants)
                .topPerformers(topPerformers)
                .userRanking(userRanking)
                .statistics(statistics)
                .build();
    }

    @Override
    @Scheduled(fixedRate = 300000)
    public void cleanupExpiredSessions() {
        log.debug("Running cleanup of expired quiz sessions");

        try {
            LocalDateTime cutOffTime = LocalDateTime.now().minusMinutes(SESSION_TIMEOUT_MINUTES);
            List<UserQuizAttempt> expiredSessions = userQuizAttemptRepository
                    .findExpiredActiveSessions(cutOffTime);

            for (UserQuizAttempt attempt : expiredSessions) {
                attempt.setStatus(QuizStatus.EXPIRED);
                userQuizAttemptRepository.save(attempt);

                quizRedisService.removeSession(attempt.getSessionId());
            }

            if (!expiredSessions.isEmpty()) {
                log.info("Cleaned up {} expired quiz sessions", expiredSessions.size());
            }
        } catch (Exception e) {
            log.error("Error during quiz session cleanup: {}", e.getMessage());
        }
    }

    @Override
    @Cacheable(value = "quizStats", key = "#date")
    public QuizStatistics getQuizStatistics(LocalDate date) {
        log.debug("Getting quiz statistics for date: {}", date);

        Optional<DailyQuiz> quizOptional = dailyQuizRepository.findByQuizDate(date);
        if (quizOptional.isEmpty()) {
            throw new CustomException("No quiz found for the specified date");
        }

        return buildQuizStatistics(quizOptional.get());
    }

    @Cacheable(value = "quizQuestions", key = "#quiz.id + '_' + #questionNumber")
    private CivicQuestion getQuestionByNumber(DailyQuiz quiz, int questionNumber) {
        return civicQuestionRepository
                .findByQuizAndNumber(quiz, questionNumber)
                .orElseThrow(() -> new CustomException("Question " + questionNumber + " not found"));
    }

    private String generateSessionId() {
        return "civic_" + UUID.randomUUID().toString().replace("-", "").substring(0, 16);
    }

    private UserQuizAttempt createQuizAttempt(User user, DailyQuiz quiz, String sessionId) {
        return UserQuizAttempt.builder()
                .user(user)
                .dailyQuiz(quiz)
                .sessionId(sessionId)
                .status(QuizStatus.ACTIVE)
                .totalQuestions(quiz.getTotalQuestions())
                .questionsAnswered(0)
                .correctAnswers(0)
                .score(0)
                .build();
    }

    private CivicQuestionResponse buildQuestionResponseForUser(CivicQuestion question) {
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

    private UserQuizSummary buildUserQuizSummary(UserQuizAttempt attempt) {
        List<UserAnswer> answers = userAnswerRepository.findByAttemptOrderByQuestionNumber(attempt);

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

    private QuestionResultSummary buildQuestionResultSummary(UserAnswer answer) {
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

    private String getOptionText(CivicQuestion question, String optionLetter) {
        return question.getOptions().stream()
                .filter(option -> option.getOptionLetter().equals(optionLetter))
                .findFirst()
                .map(QuestionOption::getOptionText)
                .orElse("");
    }

    private CategoryPerformance buildCategoryPerformance(List<UserAnswer> answers) {
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

    private LeaderboardEntry buildLeaderboardEntry(UserQuizAttempt attempt, int rank, Long currentUserId) {
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

    private QuizStatistics buildQuizStatistics(DailyQuiz quiz) {
        long totalAttempts = userQuizAttemptRepository.countTotalAttemptsByQuiz(quiz);
        long completedAttempts = userQuizAttemptRepository.countCompletedAttemptsByQuiz(quiz);
        Double averageScore = userQuizAttemptRepository.findAverageScoreByQuiz(quiz);
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