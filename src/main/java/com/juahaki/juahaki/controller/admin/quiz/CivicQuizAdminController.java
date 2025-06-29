package com.juahaki.juahaki.controller.admin.quiz;

import com.juahaki.juahaki.response.ApiResponse;
import com.juahaki.juahaki.service.quiz.CivicQuizAIService;
import com.juahaki.juahaki.service.quiz.ICivicQuizService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("${api.prefix}/admin/quiz/civic")
@RequiredArgsConstructor
@Slf4j
public class CivicQuizAdminController {

    private final CivicQuizAIService civicQuizAIService;
    private final ICivicQuizService civicQuizService;


    @PostMapping("/generate")
    public ResponseEntity<ApiResponse> generateQuiz(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(defaultValue = "10") int questionCount) {

        log.info("Admin generating quiz for date: {} with {} questions", date, questionCount);

        try {
            var quiz = civicQuizAIService.generateDailyQuiz(date, questionCount);
            return ResponseEntity.ok(new ApiResponse("Quiz generated successfully",
                    Map.of("quizId", quiz.getId(), "date", date, "questions", questionCount)));
        } catch (Exception e) {
            log.error("Failed to generate quiz: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().body(new ApiResponse("Failed to generate quiz: " + e.getMessage(), null));
        }
    }

    @PostMapping("/regenerate")
    public ResponseEntity<ApiResponse> regenerateQuiz(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam String reason) {

        log.info("Admin regenerating quiz for date: {}, reason: {}", date, reason);

        try {
            var quiz = civicQuizAIService.regenerateQuiz(date, reason);
            return ResponseEntity.ok(new ApiResponse("Quiz regenerated successfully",
                    Map.of("quizId", quiz.getId(), "date", date, "reason", reason)));
        } catch (Exception e) {
            log.error("Failed to regenerate quiz: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().body(new ApiResponse("Failed to regenerate quiz: " + e.getMessage(), null));
        }
    }

    @GetMapping("/topics/suggested")
    public ResponseEntity<ApiResponse> getSuggestedTopics() {
        log.info("Getting suggested quiz topics");

        List<String> topics = civicQuizAIService.getSuggestedTopics();
        return ResponseEntity.ok(new ApiResponse("Suggested topics retrieved successfully", topics));
    }

    @GetMapping("/quiz/{quizId}/quality-analysis")
    public ResponseEntity<ApiResponse> analyzeQuizQuality(@PathVariable Long quizId) {
        log.info("Analyzing quiz quality for quiz ID: {}", quizId);

        try {
            var analysis = civicQuizAIService.analyzeQuizQuality(quizId);
            return ResponseEntity.ok(new ApiResponse("Quiz quality analysis completed", analysis));
        } catch (Exception e) {
            log.error("Failed to analyze quiz quality: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().body(new ApiResponse("Failed to analyze quiz: " + e.getMessage(), null));
        }
    }

    @PostMapping("/admin/maintenance/cleanup-expired")
    public ResponseEntity<ApiResponse> cleanupExpiredSessions() {
        log.info("Admin triggering cleanup of expired sessions");

        try {
            civicQuizService.cleanupExpiredSessions();
            return ResponseEntity.ok(new ApiResponse("Expired sessions cleanup completed", null));
        } catch (Exception e) {
            log.error("Failed to cleanup expired sessions: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().body(new ApiResponse("Cleanup failed: " + e.getMessage(), null));
        }
    }

}
