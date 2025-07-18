package com.juahaki.juahaki.service.ai.quiz;

import com.juahaki.juahaki.dto.quiz.civic.*;
import jakarta.servlet.http.HttpServletRequest;

import java.time.LocalDate;
import java.util.List;

public interface ICivicQuizService {

    List<UserQuizMetadata> getUserQuizHistoryMetadata(HttpServletRequest request);


    UserQuizSummary getQuizDetailsBySessionId(String sessionId, HttpServletRequest request);

    CivicQuizInfoResponse getTodaysInfo(HttpServletRequest request);

    CivicQuizInfoResponse getQuizInfo(LocalDate date, HttpServletRequest request);

    StartCivicQuizResponse startQuiz(HttpServletRequest request);

    SubmitCivicAnswerResponse submitAnswer(SubmitCivicAnswerRequest request, HttpServletRequest httpServletRequest);

    CivicQuizSessionResponse getSessionStatus(String sessionId, HttpServletRequest request);

    void abandonSession(String sessionId, HttpServletRequest request);


    // Results and statistics
    UserQuizSummary getQuizResults(String sessionId, HttpServletRequest request);

    QuizLeaderboardResponse getTodaysLeaderboard(HttpServletRequest request);

    QuizLeaderboardResponse getQuizLeaderboardResponse(LocalDate date, HttpServletRequest request);


    //Admin Operations
    void cleanupExpiredSessions();

    QuizStatistics getQuizStatistics(LocalDate date);
}
