package com.juahaki.juahaki.service.quiz;

import com.juahaki.juahaki.dto.quiz.civic.*;
import com.juahaki.juahaki.enums.QuizStatus;
import com.juahaki.juahaki.exception.CustomException;
import com.juahaki.juahaki.mapper.QuizEntityMapper;
import com.juahaki.juahaki.mapper.QuizResponseMapper;
import com.juahaki.juahaki.model.quiz.*;
import com.juahaki.juahaki.model.user.User;
import com.juahaki.juahaki.repository.quiz.*;
import com.juahaki.juahaki.repository.user.UserRepository;
import com.juahaki.juahaki.service.ai.quiz.ICivicQuizService;
import com.juahaki.juahaki.util.jwt.JwtHelperService;
import com.juahaki.juahaki.util.quiz.QuizSessionBuilder;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.redis.core.RedisTemplate;
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
    private final RedisTemplate<String, Object> redisTemplate;

    private final QuizResponseMapper responseMapper;
    private final QuizEntityMapper entityMapper;

    private static final long SESSION_TIMEOUT_MINUTES = 600;
    private static final String QUIZ_QUESTIONS_PREFIX = "quiz:questions:";

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
        cacheQuizQuestionsInRedis(quiz);

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
            cacheQuizQuestionsInRedis(todaysQuiz);

            String sessionId = QuizSessionBuilder.generateSessionId();
            UserQuizAttempt attempt = entityMapper.createQuizAttempt(user, todaysQuiz, sessionId);
            UserQuizAttempt savedAttempt = userQuizAttemptRepository.save(attempt);

            CivicQuizSessionData sessionData = QuizSessionBuilder.createSessionData(
                    userId, todaysQuiz.getId(), savedAttempt.getId());

            quizRedisService.storeCivicQuizSession(sessionId, sessionData,
                    SESSION_TIMEOUT_MINUTES, TimeUnit.MINUTES);

            CivicQuestion firstQuestion = getQuestionFromRedis(todaysQuiz.getId(), 1);
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

        Optional<CivicQuizSessionData> sessionOptional =
                quizRedisService.getCivicQuizSession(request.getSessionId());

        if (sessionOptional.isEmpty()) {
            return QuizSessionBuilder.buildSubmitAnswerError("Invalid or expired session");
        }

        CivicQuizSessionData sessionData = sessionOptional.get();

        if (!sessionData.getUserId().equals(userId)) {
            return QuizSessionBuilder.buildSubmitAnswerError("Session does not belong to current user");
        }

        try {
            UserQuizAttempt attempt = userQuizAttemptRepository.findById(sessionData.getAttemptId())
                    .orElseThrow(() -> new CustomException("Quiz attempt not found"));

            if (attempt.getStatus() != QuizStatus.ACTIVE) {
                return QuizSessionBuilder.buildSubmitAnswerError("Quiz session is not active");
            }

            CivicQuestion currentQuestion = getQuestionFromRedis(attempt.getDailyQuiz().getId(),
                    sessionData.getCurrentQuestionNumber());

            if (currentQuestion == null) {
                return QuizSessionBuilder.buildSubmitAnswerError("Question not found");
            }

            // Check if already answered
            if (userAnswerRepository.existsByAttemptAndQuestion(attempt, currentQuestion)) {
                return QuizSessionBuilder.buildSubmitAnswerError("Question already answered");
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

            // Update attempt
            attempt.setQuestionsAnswered(attempt.getQuestionsAnswered() + 1);
            if (isCorrect) {
                attempt.setCorrectAnswers(attempt.getCorrectAnswers() + 1);
            }

            boolean hasNextQuestion = sessionData.getCurrentQuestionNumber() < attempt.getTotalQuestions();
            CivicQuestionResponse nextQuestion = null;
            UserQuizSummary finalResults = null;

            if (hasNextQuestion) {
                // Move to next question
                QuizSessionBuilder.moveToNextQuestion(sessionData);
                quizRedisService.updateCivicQuizSession(request.getSessionId(), sessionData,
                        SESSION_TIMEOUT_MINUTES, TimeUnit.MINUTES);

                // Get next question from Redis
                CivicQuestion next = getQuestionFromRedis(attempt.getDailyQuiz().getId(),
                        sessionData.getCurrentQuestionNumber());

                if (next != null) {
                    nextQuestion = responseMapper.buildQuestionResponseForUser(next);
                }
            } else {
                // Quiz completed
                attempt.completeQuiz();
                List<UserAnswer> allAnswers = userAnswerRepository.findByAttemptOrderByQuestionNumber(attempt);
                finalResults = responseMapper.buildUserQuizSummary(attempt, allAnswers);

                // Remove session from Redis
                quizRedisService.removeCivicQuizSession(request.getSessionId());

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

        Optional<CivicQuizSessionData> sessionOptional =
                quizRedisService.getCivicQuizSession(sessionId);

        if (sessionOptional.isEmpty()) {
            return QuizSessionBuilder.buildSessionStatusError("Session not found or expired");
        }

        CivicQuizSessionData sessionData = sessionOptional.get();

        if (!sessionData.getUserId().equals(userId)) {
            return QuizSessionBuilder.buildSessionStatusError("Session does not belong to current user");
        }

        try {
            UserQuizAttempt attempt = userQuizAttemptRepository.findById(sessionData.getAttemptId())
                    .orElseThrow(() -> new CustomException("Quiz attempt not found"));

            if (QuizSessionBuilder.isSessionExpired(attempt, SESSION_TIMEOUT_MINUTES)) {
                attempt.setStatus(QuizStatus.EXPIRED);
                userQuizAttemptRepository.save(attempt);
                quizRedisService.removeCivicQuizSession(sessionId);
                return QuizSessionBuilder.buildSessionStatusError("Session has expired");
            }

            // Get current question from Redis
            CivicQuestion currentQuestion = getQuestionFromRedis(attempt.getDailyQuiz().getId(),
                    sessionData.getCurrentQuestionNumber());

            CivicQuestionResponse questionResponse = currentQuestion != null ?
                    responseMapper.buildQuestionResponseForUser(currentQuestion) : null;

            long ttl = quizRedisService.getSessionTTL(sessionId);
            String timeRemaining = ttl > 0 ? formatDuration(Duration.ofSeconds(ttl)) : "Expired";

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

        DailyQuiz quiz = quizOptional.get();
        long totalAttempts = userQuizAttemptRepository.countTotalAttemptsByQuiz(quiz);
        long completedAttempts = userQuizAttemptRepository.countCompletedAttemptsByQuiz(quiz);
        Double averageScore = userQuizAttemptRepository.findAverageScoreByQuiz(quiz);

        return responseMapper.buildQuizStatistics(quiz, totalAttempts, completedAttempts, averageScore);
    }

    // Redis operations for question caching
    private void cacheQuizQuestionsInRedis(DailyQuiz quiz) {
        String questionsKey = QUIZ_QUESTIONS_PREFIX + quiz.getId();

        try {
            if (redisTemplate.hasKey(questionsKey)) {
                log.debug("Questions for quiz {} already cached in Redis", quiz.getId());
                return;
            }

            List<CivicQuestion> questions = civicQuestionRepository.findByDailyQuizOrderByQuestionNumber(quiz);

            if (questions.isEmpty()) {
                log.warn("No questions found for quiz {}", quiz.getId());
                return;
            }

            Map<String, Object> questionMap = new HashMap<>();
            for (CivicQuestion question : questions) {
                RedisQuestionDto redisDto = entityMapper.convertToRedisDto(question);
                questionMap.put(String.valueOf(question.getQuestionNumber()), redisDto);
            }

            redisTemplate.opsForHash().putAll(questionsKey, questionMap);

            Duration timeUntilExpiry = Duration.between(LocalDateTime.now(), quiz.getExpiresAt());
            if (!timeUntilExpiry.isNegative() && !timeUntilExpiry.isZero()) {
                redisTemplate.expire(questionsKey, timeUntilExpiry);
            }

            log.info("Cached {} questions for quiz {} in Redis with expiry: {}",
                    questions.size(), quiz.getId(), quiz.getExpiresAt());

        } catch (Exception e) {
            log.error("Failed to cache questions for quiz {} in Redis: {}", quiz.getId(), e.getMessage(), e);
        }
    }

    private CivicQuestion getQuestionFromRedis(Long quizId, int questionNumber) {
        String questionsKey = QUIZ_QUESTIONS_PREFIX + quizId;

        try {
            Object questionObj = redisTemplate.opsForHash().get(questionsKey, String.valueOf(questionNumber));

            if (questionObj instanceof RedisQuestionDto) {
                log.debug("Retrieved question {} for quiz {} from Redis", questionNumber, quizId);
                return entityMapper.convertFromRedisDto((RedisQuestionDto) questionObj);
            }

            log.warn("Question {} not found in Redis for quiz {}", questionNumber, quizId);
            return null;

        } catch (Exception e) {
            log.error("Failed to get question {} from Redis for quiz {}: {}",
                    questionNumber, quizId, e.getMessage(), e);
            return null;
        }
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