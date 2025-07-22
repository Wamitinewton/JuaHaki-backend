package com.juahaki.juahaki.core.quiz.controller.admin;

import com.juahaki.juahaki.shared.dto.response.ApiResponse;
import com.juahaki.juahaki.core.quiz.service.ai.CivicQuizAIService;
import com.juahaki.juahaki.core.quiz.service.ICivicQuizService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
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
