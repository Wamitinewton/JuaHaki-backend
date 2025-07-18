package com.juahaki.juahaki.controller.quiz;

import com.juahaki.juahaki.dto.quiz.civic.*;
import com.juahaki.juahaki.response.ApiResponse;
import com.juahaki.juahaki.service.quiz.ICivicQuizService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("${api.prefix}/users/quiz/civic")
@RequiredArgsConstructor
@Slf4j
public class CivicQuizController {

    private final ICivicQuizService civicQuizService;

    @GetMapping("/today")
    public ResponseEntity<ApiResponse> getTodaysQuiz(HttpServletRequest request) {
        log.info("Getting today's quiz info");

        CivicQuizInfoResponse quizInfo = civicQuizService.getTodaysInfo(request);
        return ResponseEntity.ok(new ApiResponse("Today's quiz information retrieved successfully", quizInfo));
    }

    @GetMapping("/info")
    public ResponseEntity<ApiResponse> getQuizInfo(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            HttpServletRequest request) {

        log.info("Getting quiz info for date: {}", date);

        CivicQuizInfoResponse quizInfo = civicQuizService.getQuizInfo(date, request);
        return ResponseEntity.ok(new ApiResponse("Quiz information retrieved successfully", quizInfo));
    }

    @PostMapping("/start")
    public ResponseEntity<ApiResponse> startQuiz(HttpServletRequest request) {
        log.info("Starting civic quiz");

        StartCivicQuizResponse response = civicQuizService.startQuiz(request);

        if (response.isSuccessful()) {
            return ResponseEntity.ok(new ApiResponse("Quiz started successfully", response));
        } else {
            return ResponseEntity.badRequest().body(new ApiResponse(response.getErrorMessage(), null));
        }
    }

    @PostMapping("/submit-answer")
    public ResponseEntity<ApiResponse> submitAnswer(
            @Valid @RequestBody SubmitCivicAnswerRequest submitRequest,
            HttpServletRequest request) {

        log.info("Submitting answer for session: {}", submitRequest.getSessionId());

        SubmitCivicAnswerResponse response = civicQuizService.submitAnswer(submitRequest, request);

        if (response.isSuccessful()) {
            return ResponseEntity.ok(new ApiResponse("Answer submitted successfully", response));
        } else {
            return ResponseEntity.badRequest().body(new ApiResponse(response.getErrorMessage(), null));
        }
    }

    @GetMapping("/session/{sessionId}/status")
    public ResponseEntity<ApiResponse> getSessionStatus(
            @PathVariable String sessionId,
            HttpServletRequest request) {

        log.info("Getting session status for: {}", sessionId);

        CivicQuizSessionResponse response = civicQuizService.getSessionStatus(sessionId, request);

        if (response.isSuccessful()) {
            return ResponseEntity.ok(new ApiResponse("Session status retrieved successfully", response));
        } else {
            return ResponseEntity.badRequest().body(new ApiResponse(response.getErrorMessage(), null));
        }
    }

    @PostMapping("/session/{sessionId}/abandon")
    public ResponseEntity<ApiResponse> abandonSession(
            @PathVariable String sessionId,
            HttpServletRequest request) {

        log.info("Abandoning session: {}", sessionId);

        civicQuizService.abandonSession(sessionId, request);
        return ResponseEntity.ok(new ApiResponse("Session abandoned successfully", null));
    }

    @GetMapping("/results/{sessionId}")
    public ResponseEntity<ApiResponse> getQuizResults(
            @PathVariable String sessionId,
            HttpServletRequest request) {

        log.info("Getting quiz results for session: {}", sessionId);

        UserQuizSummary results = civicQuizService.getQuizResults(sessionId, request);
        return ResponseEntity.ok(new ApiResponse("Quiz results retrieved successfully", results));
    }

    @GetMapping("/history/metadata")
    public ResponseEntity<ApiResponse> getUserQuizHistoryMetadata(HttpServletRequest request) {
        log.info("Getting user quiz history metadata");

        List<UserQuizMetadata> historyMetadata = civicQuizService.getUserQuizHistoryMetadata(request);
        return ResponseEntity.ok(new ApiResponse("Quiz history metadata retrieved successfully", historyMetadata));
    }

    @GetMapping("/history/details/{sessionId}")
    public ResponseEntity<ApiResponse> getQuizDetailsBySessionId(
            @PathVariable String sessionId,
            HttpServletRequest request) {

        log.info("Getting detailed quiz results for session: {}", sessionId);

        UserQuizSummary quizDetails = civicQuizService.getQuizDetailsBySessionId(sessionId, request);
        return ResponseEntity.ok(new ApiResponse("Quiz details retrieved successfully", quizDetails));
    }

    @GetMapping("/leaderboard/today")
    public ResponseEntity<ApiResponse> getTodaysLeaderboard(HttpServletRequest request) {
        log.info("Getting today's leaderboard");

        QuizLeaderboardResponse leaderboard = civicQuizService.getTodaysLeaderboard(request);
        return ResponseEntity.ok(new ApiResponse("Today's leaderboard retrieved successfully", leaderboard));
    }

    @GetMapping("/leaderboard")
    public ResponseEntity<ApiResponse> getLeaderboard(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            HttpServletRequest request) {

        log.info("Getting leaderboard for date: {}", date);

        QuizLeaderboardResponse leaderboard = civicQuizService.getQuizLeaderboardResponse(date, request);
        return ResponseEntity.ok(new ApiResponse("Leaderboard retrieved successfully", leaderboard));
    }

    @GetMapping("/statistics")
    public ResponseEntity<ApiResponse> getQuizStatistics(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {

        log.info("Getting quiz statistics for date: {}", date);

        QuizStatistics statistics = civicQuizService.getQuizStatistics(date);
        return ResponseEntity.ok(new ApiResponse("Quiz statistics retrieved successfully", statistics));
    }


}
