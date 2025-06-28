package com.juahaki.juahaki.service.quiz;

import com.juahaki.juahaki.dto.quiz.civic.*;
import com.juahaki.juahaki.repository.quiz.CivicQuestionRepository;
import com.juahaki.juahaki.repository.quiz.DailyQuizRepository;
import com.juahaki.juahaki.repository.quiz.UserAnswerRepository;
import com.juahaki.juahaki.repository.quiz.UserQuizAttemptRepository;
import com.juahaki.juahaki.repository.user.UserRepository;
import com.juahaki.juahaki.util.jwt.JwtHelperService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

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
    private final RedisTemplate<String, Object> redisTemplate;


    @Override
    public CivicQuizInfoResponse getTodaysInfo(HttpServletRequest request) {
        return null;
    }

    @Override
    public CivicQuizInfoResponse getQuizInfo(LocalDate date, HttpServletRequest request) {
        return null;
    }

    @Override
    public StartCivicQuizResponse startQuiz(HttpServletRequest request) {
        return null;
    }

    @Override
    public SubmitCivicAnswerRequest submitAnswer(SubmitCivicAnswerRequest request, HttpServletRequest httpServletRequest) {
        return null;
    }

    @Override
    public CivicQuizSessionResponse getSessionStatus(String sessionId, HttpServletRequest request) {
        return null;
    }

    @Override
    public void abandonSession(String sessionId, HttpServletRequest request) {

    }

    @Override
    public UserQuizSummary getQuizResults(String sessionId, HttpServletRequest request) {
        return null;
    }

    @Override
    public List<UserQuizSummary> getUserQuizHistory(HttpServletRequest request) {
        return List.of();
    }

    @Override
    public QuizLeaderboardResponse getTodaysLeaderboard(HttpServletRequest request) {
        return null;
    }

    @Override
    public QuizLeaderboardResponse getQuizLeaderboardResponse(LocalDate date, HttpServletRequest request) {
        return null;
    }

    @Override
    public void cleanupExpiredSessions() {

    }

    @Override
    public QuizStatistics getQuizStatistics(LocalDate date) {
        return null;
    }
}
