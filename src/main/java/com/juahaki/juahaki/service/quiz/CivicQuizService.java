package com.juahaki.juahaki.service.quiz;

import com.juahaki.juahaki.dto.quiz.civic.*;
import com.juahaki.juahaki.enums.QuizStatus;
import com.juahaki.juahaki.exception.CustomException;
import com.juahaki.juahaki.mapper.QuizEntityMapper;
import com.juahaki.juahaki.mapper.QuizResponseMapper;
import com.juahaki.juahaki.model.quiz.CivicQuestion;
import com.juahaki.juahaki.model.quiz.DailyQuiz;
import com.juahaki.juahaki.model.quiz.UserAnswer;
import com.juahaki.juahaki.model.quiz.UserQuizAttempt;
import com.juahaki.juahaki.model.user.User;
import com.juahaki.juahaki.repository.quiz.CivicQuestionRepository;
import com.juahaki.juahaki.repository.quiz.DailyQuizRepository;
import com.juahaki.juahaki.repository.quiz.UserAnswerRepository;
import com.juahaki.juahaki.repository.quiz.UserQuizAttemptRepository;
import com.juahaki.juahaki.repository.user.UserRepository;
import com.juahaki.juahaki.util.jwt.JwtHelperService;
import com.juahaki.juahaki.util.quiz.QuizSessionBuilder;
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
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class CivicQuizService implements ICivicQuizService {

    private static final long SESSION_TIMEOUT_MINUTES = 600;
    private final DailyQuizRepository dailyQuizRepository;
    private final CivicQuestionRepository civicQuestionRepository;
    private final UserQuizAttemptRepository userQuizAttemptRepository;
    private final UserAnswerRepository userAnswerRepository;
    private final UserRepository userRepository;
    private final JwtHelperService jwtHelperService;
    private final QuizResponseMapper responseMapper;
    private final QuizEntityMapper entityMapper;

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
        User user = userRepository.findById(userId).orElseThrow();

        Optional<DailyQuiz> quizOptional = dailyQuizRepository.findActiveQuizByDate(date);
        if (quizOptional.isEmpty()) {
            throw new CustomException("No quiz available for the selected date");
        }

        DailyQuiz quiz = quizOptional.get();

        Optional<UserQuizAttempt> userAttempt = userQuizAttemptRepository.findByUserAndDailyQuiz(user, quiz);
        UserQuizSummary lastAttempt = null;

        if (userAttempt.isPresent()) {
            List<UserAnswer> answers = userAnswerRepository.findByAttemptOrderByQuestionNumber(userAttempt.get());
            lastAttempt = responseMapper.buildUserQuizSummary(userAttempt.get(), answers);
        }

        return responseMapper.buildQuizInfoResponse(quiz, userAttempt.isPresent(), lastAttempt);
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
            return QuizSessionBuilder.buildStartQuizError("Today's quiz has expired");
        }

        if (userQuizAttemptRepository.existsByUserAndDailyQuiz(user, todaysQuiz)) {
            return QuizSessionBuilder.buildStartQuizError("You have already attempted today's quiz");
        }

        try {
            String sessionId = QuizSessionBuilder.generateSessionId();
            UserQuizAttempt attempt = entityMapper.createQuizAttempt(user, todaysQuiz, sessionId);
            userQuizAttemptRepository.save(attempt);

            CivicQuestion firstQuestion = getQuestionFromDatabase(todaysQuiz.getId(), 1);
            if (firstQuestion == null) {
                return QuizSessionBuilder.buildStartQuizError("Questions not available. Please try again.");
            }

            CivicQuestionResponse questionResponse = responseMapper.buildQuestionResponseForUser(firstQuestion);

            log.info("Started quiz session {} for user {}", sessionId, userId);
            return QuizSessionBuilder.buildStartQuizSuccess(sessionId, todaysQuiz, questionResponse);

        } catch (Exception e) {
            log.error("Error starting quiz for user {}: {}", userId, e.getMessage(), e);
            return QuizSessionBuilder.buildStartQuizError("Failed to start quiz: " + e.getMessage());
        }
    }

    @Override
    @Transactional
    @CacheEvict(value = {"leaderboard", "quizStats", "userQuizHistory"}, allEntries = true)
    public SubmitCivicAnswerResponse submitAnswer(SubmitCivicAnswerRequest request,
                                                  HttpServletRequest httpRequest) {
        log.debug("Submitting answer for session: {}", request.getSessionId());

        Long userId = jwtHelperService.getCurrentUserIdFromRequest(httpRequest);

        Optional<UserQuizAttempt> attemptOptional = userQuizAttemptRepository.findBySessionId(request.getSessionId());
        if (attemptOptional.isEmpty()) {
            return QuizSessionBuilder.buildSubmitAnswerError("Invalid or expired session");
        }

        UserQuizAttempt attempt = attemptOptional.get();

        if (!attempt.getUser().getId().equals(userId)) {
            return QuizSessionBuilder.buildSubmitAnswerError("Session does not belong to current user");
        }

        if (attempt.getStatus() != QuizStatus.ACTIVE) {
            return QuizSessionBuilder.buildSubmitAnswerError("Quiz session is not active");
        }

        if (isSessionExpired(attempt)) {
            attempt.setStatus(QuizStatus.EXPIRED);
            userQuizAttemptRepository.save(attempt);
            return QuizSessionBuilder.buildSubmitAnswerError("Quiz session has expired");
        }

        try {
            int currentQuestionNumber = attempt.getQuestionsAnswered() + 1;
            CivicQuestion currentQuestion = getQuestionFromDatabase(attempt.getDailyQuiz().getId(), currentQuestionNumber);

            if (currentQuestion == null) {
                return QuizSessionBuilder.buildSubmitAnswerError("Question not found");
            }

            if (userAnswerRepository.existsByAttemptAndQuestion(attempt, currentQuestion)) {
                return QuizSessionBuilder.buildSubmitAnswerError("Question already answered");
            }

            boolean isCorrect = currentQuestion.getCorrectAnswer().equals(request.getAnswer());

            UserAnswer userAnswer = UserAnswer.builder()
                    .attempt(attempt)
                    .question(currentQuestion)
                    .selectedAnswer(request.getAnswer())
                    .isCorrect(isCorrect)
                    .timeSpentSeconds(request.getTimeSpentSeconds())
                    .build();
            userAnswerRepository.save(userAnswer);

            attempt.setQuestionsAnswered(attempt.getQuestionsAnswered() + 1);
            if (isCorrect) {
                attempt.setCorrectAnswers(attempt.getCorrectAnswers() + 1);
            }

            boolean hasNextQuestion = attempt.getQuestionsAnswered() < attempt.getTotalQuestions();
            CivicQuestionResponse nextQuestion = null;
            UserQuizSummary finalResults = null;

            if (hasNextQuestion) {
                CivicQuestion next = getQuestionFromDatabase(attempt.getDailyQuiz().getId(),
                        attempt.getQuestionsAnswered() + 1);

                if (next != null) {
                    nextQuestion = responseMapper.buildQuestionResponseForUser(next);
                }
            } else {
                attempt.completeQuiz();
                List<UserAnswer> allAnswers = userAnswerRepository.findByAttemptOrderByQuestionNumber(attempt);
                finalResults = responseMapper.buildUserQuizSummary(attempt, allAnswers);

                log.info("Quiz completed for user {} with score {}", userId, attempt.getScore());
            }

            userQuizAttemptRepository.save(attempt);

            SubmitCivicAnswerResponse response = QuizSessionBuilder.buildSubmitAnswerSuccess(
                    isCorrect, currentQuestion.getCorrectAnswer(), currentQuestion.getCorrectOptionText(),
                    currentQuestion.getExplanation(), attempt, hasNextQuestion, nextQuestion, finalResults);

            return response.forUser();

        } catch (Exception e) {
            log.error("Error submitting answer for session {}: {}", request.getSessionId(), e.getMessage(), e);
            return QuizSessionBuilder.buildSubmitAnswerError("Failed to submit answer: " + e.getMessage());
        }
    }

    @Override
    public CivicQuizSessionResponse getSessionStatus(String sessionId, HttpServletRequest request) {
        log.debug("Getting session status for: {}", sessionId);

        Long userId = jwtHelperService.getCurrentUserIdFromRequest(request);

        Optional<UserQuizAttempt> attemptOptional = userQuizAttemptRepository.findBySessionId(sessionId);
        if (attemptOptional.isEmpty()) {
            return QuizSessionBuilder.buildSessionStatusError("Session not found or expired");
        }

        UserQuizAttempt attempt = attemptOptional.get();

        if (!attempt.getUser().getId().equals(userId)) {
            return QuizSessionBuilder.buildSessionStatusError("Session does not belong to current user");
        }

        try {
            if (isSessionExpired(attempt)) {
                attempt.setStatus(QuizStatus.EXPIRED);
                userQuizAttemptRepository.save(attempt);
                return QuizSessionBuilder.buildSessionStatusError("Session has expired");
            }

            int currentQuestionNumber = attempt.getQuestionsAnswered() + 1;
            CivicQuestion currentQuestion = getQuestionFromDatabase(attempt.getDailyQuiz().getId(), currentQuestionNumber);

            CivicQuestionResponse questionResponse = currentQuestion != null ?
                    responseMapper.buildQuestionResponseForUser(currentQuestion) : null;

            Duration timeLeft = Duration.between(LocalDateTime.now(),
                    attempt.getStartedAt().plusMinutes(SESSION_TIMEOUT_MINUTES));
            String timeRemaining = timeLeft.isNegative() ? "Expired" : formatDuration(timeLeft);

            return QuizSessionBuilder.buildSessionStatusSuccess(sessionId, attempt, questionResponse, timeRemaining);

        } catch (Exception e) {
            log.error("Error getting session status for {}: {}", sessionId, e.getMessage(), e);
            return QuizSessionBuilder.buildSessionStatusError("Failed to get session status");
        }
    }

    @Override
    @Transactional
    public void abandonSession(String sessionId, HttpServletRequest request) {
        Long userId = jwtHelperService.getCurrentUserIdFromRequest(request);

        Optional<UserQuizAttempt> attemptOptional = userQuizAttemptRepository.findBySessionId(sessionId);
        if (attemptOptional.isPresent()) {
            UserQuizAttempt attempt = attemptOptional.get();

            if (attempt.getUser().getId().equals(userId) && attempt.getStatus() == QuizStatus.ACTIVE) {
                attempt.setStatus(QuizStatus.ABANDONED);
                userQuizAttemptRepository.save(attempt);
                log.info("Session {} abandoned by user {}", sessionId, userId);
            }
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

        List<UserAnswer> answers = userAnswerRepository.findByAttemptOrderByQuestionNumber(attempt);
        return responseMapper.buildUserQuizSummary(attempt, answers);
    }

    @Override
    @Cacheable(value = "userQuizHistory", key = "'metadata_' + #request.getHeader('Authorization')")
    public List<UserQuizMetadata> getUserQuizHistoryMetadata(HttpServletRequest request) {
        log.debug("Getting user quiz history metadata");

        Long userId = jwtHelperService.getCurrentUserIdFromRequest(request);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException("User not found"));

        List<UserQuizAttempt> completedAttempts = userQuizAttemptRepository
                .findCompletedAttemptsByUser(user);

        return completedAttempts.stream()
                .map(responseMapper::buildUserQuizMetadata)
                .collect(Collectors.toList());
    }

    @Override
    @Cacheable(value = "quizDetails", key = "'details_' + #sessionId + '_' + #request.getHeader('Authorization')")
    public UserQuizSummary getQuizDetailsBySessionId(String sessionId, HttpServletRequest request) {
        log.debug("Getting detailed quiz results for session: {}", sessionId);

        Long userId = jwtHelperService.getCurrentUserIdFromRequest(request);

        Optional<UserQuizAttempt> attemptOptional = userQuizAttemptRepository.findBySessionId(sessionId);
        if (attemptOptional.isEmpty()) {
            throw new CustomException("Quiz results not found");
        }

        UserQuizAttempt attempt = attemptOptional.get();

        if (!attempt.getUser().getId().equals(userId)) {
            throw new CustomException("Access denied to quiz results");
        }

        if (attempt.getStatus() != QuizStatus.COMPLETED) {
            throw new CustomException("Quiz not completed yet");
        }

        List<UserAnswer> answers = userAnswerRepository.findByAttemptOrderByQuestionNumber(attempt);
        return responseMapper.buildUserQuizSummary(attempt, answers);
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
            LeaderboardEntry entry = responseMapper.buildLeaderboardEntry(attempt, i + 1, userId);
            topPerformers.add(entry);
        }

        LeaderboardEntry userRanking = null;
        Optional<UserQuizAttempt> userQuizAttempt = userQuizAttemptRepository
                .findByUserAndQuizDate(userRepository.findById(userId).orElseThrow(), date);

        if (userQuizAttempt.isPresent() && userQuizAttempt.get().getStatus() == QuizStatus.COMPLETED) {
            List<UserQuizAttempt> allAttempts = userQuizAttemptRepository.findLeaderboardByQuiz(quiz);
            int userRank = allAttempts.indexOf(userQuizAttempt.get()) + 1;
            userRanking = responseMapper.buildLeaderboardEntry(userQuizAttempt.get(), userRank, userId);
        }

        QuizStatistics statistics = getQuizStatistics(date);
        long totalParticipants = userQuizAttemptRepository.countCompletedAttemptsByQuiz(quiz);

        return QuizSessionBuilder.buildLeaderboardResponse(quiz, topPerformers, userRanking, statistics, totalParticipants);
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

        DailyQuiz quiz = quizOptional.get();
        long totalAttempts = userQuizAttemptRepository.countTotalAttemptsByQuiz(quiz);
        long completedAttempts = userQuizAttemptRepository.countCompletedAttemptsByQuiz(quiz);
        Double averageScore = userQuizAttemptRepository.findAverageScoreByQuiz(quiz);

        return responseMapper.buildQuizStatistics(quiz, totalAttempts, completedAttempts, averageScore);
    }

    private CivicQuestion getQuestionFromDatabase(Long quizId, int questionNumber) {
        Optional<DailyQuiz> quizOptional = dailyQuizRepository.findById(quizId);
        return quizOptional.flatMap(dailyQuiz -> civicQuestionRepository.findByQuizAndNumber(dailyQuiz, questionNumber)).orElse(null);

    }

    private boolean isSessionExpired(UserQuizAttempt attempt) {
        return attempt.getStatus() == QuizStatus.ACTIVE &&
                LocalDateTime.now().isAfter(attempt.getStartedAt().plusMinutes(SESSION_TIMEOUT_MINUTES));
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