package com.juahaki.juahaki.controller.quiz;

import com.juahaki.juahaki.dto.quiz.civic.*;
import com.juahaki.juahaki.response.ApiResponse;
import com.juahaki.juahaki.service.document.PdfDocumentProcessingService;
import com.juahaki.juahaki.service.quiz.CivicQuizAIService;
import com.juahaki.juahaki.service.quiz.ICivicQuizService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

@RestController
@RequestMapping("${api.prefix}/quiz/civic")
@RequiredArgsConstructor
@Slf4j
@PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
public class CivicQuizController {

    private final ICivicQuizService civicQuizService;
    private final CivicQuizAIService civicQuizAIService;
    private final PdfDocumentProcessingService pdfDocumentProcessingService;


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

    @GetMapping("/history")
    public ResponseEntity<ApiResponse> getUserQuizHistory(HttpServletRequest request) {
        log.info("Getting user quiz history");

        List<UserQuizSummary> history = civicQuizService.getUserQuizHistory(request);
        return ResponseEntity.ok(new ApiResponse("Quiz history retrieved successfully", history));
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


    @PostMapping("/admin/generate")
    @PreAuthorize("hasRole('ADMIN')")
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

    @PostMapping("/admin/regenerate")
    @PreAuthorize("hasRole('ADMIN')")
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

    @GetMapping("/admin/topics/suggested")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse> getSuggestedTopics() {
        log.info("Getting suggested quiz topics");

        List<String> topics = civicQuizAIService.getSuggestedTopics();
        return ResponseEntity.ok(new ApiResponse("Suggested topics retrieved successfully", topics));
    }

    @GetMapping("/admin/quiz/{quizId}/quality-analysis")
    @PreAuthorize("hasRole('ADMIN')")
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

    // === Document Management Endpoints ===

    @PostMapping("/admin/documents/upload")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse> uploadDocument(
            @RequestParam("file") MultipartFile file,
            @RequestParam(required = false) Map<String, String> metadata) {

        log.info("Admin uploading document: {}", file.getOriginalFilename());

        try {
            var result = pdfDocumentProcessingService.uploadAndProcessDocument(file, metadata);

            if (result.isSuccess()) {
                return ResponseEntity.ok(new ApiResponse("Document uploaded and processed successfully", result));
            } else {
                return ResponseEntity.badRequest().body(new ApiResponse(result.getMessage(), result));
            }
        } catch (Exception e) {
            log.error("Failed to upload document: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().body(new ApiResponse("Failed to upload document: " + e.getMessage(), null));
        }
    }

    @PostMapping("/admin/documents/process-all")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse> processAllDocuments() {
        log.info("Admin initiating batch processing of all documents");

        try {
            CompletableFuture<PdfDocumentProcessingService.BatchProcessingResult> future =
                    pdfDocumentProcessingService.processAllFirebaseDocuments();

            return ResponseEntity.ok(new ApiResponse("Batch processing initiated",
                    Map.of("status", "processing", "message", "Processing will continue in background")));
        } catch (Exception e) {
            log.error("Failed to initiate batch processing: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().body(new ApiResponse("Failed to start batch processing: " + e.getMessage(), null));
        }
    }

    @PostMapping("/admin/documents/{storagePath}/reprocess")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse> reprocessDocument(@PathVariable String storagePath) {
        log.info("Admin reprocessing document: {}", storagePath);

        try {
            var result = pdfDocumentProcessingService.reprocessDocument(storagePath);

            if (result.isSuccess()) {
                return ResponseEntity.ok(new ApiResponse("Document reprocessed successfully", result));
            } else {
                return ResponseEntity.badRequest().body(new ApiResponse(result.getMessage(), result));
            }
        } catch (Exception e) {
            log.error("Failed to reprocess document: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().body(new ApiResponse("Failed to reprocess document: " + e.getMessage(), null));
        }
    }

    @DeleteMapping("/admin/documents/{storagePath}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse> deleteDocument(@PathVariable String storagePath) {
        log.info("Admin deleting document: {}", storagePath);

        try {
            boolean deleted = pdfDocumentProcessingService.deleteDocument(storagePath);

            if (deleted) {
                return ResponseEntity.ok(new ApiResponse("Document deleted successfully",
                        Map.of("storagePath", storagePath, "deleted", true)));
            } else {
                return ResponseEntity.badRequest().body(new ApiResponse("Failed to delete document",
                        Map.of("storagePath", storagePath, "deleted", false)));
            }
        } catch (Exception e) {
            log.error("Failed to delete document: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().body(new ApiResponse("Failed to delete document: " + e.getMessage(), null));
        }
    }

    @GetMapping("/admin/documents/statistics")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse> getDocumentStatistics() {
        log.info("Getting document processing statistics");

        var statistics = pdfDocumentProcessingService.getProcessingStatistics();
        return ResponseEntity.ok(new ApiResponse("Document statistics retrieved successfully", statistics));
    }

    @GetMapping("/admin/documents/search")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse> searchDocuments(
            @RequestParam String query,
            @RequestParam(defaultValue = "10") int limit,
            @RequestParam(defaultValue = "0.6") double similarityThreshold) {

        log.info("Searching documents with query: '{}', limit: {}", query, limit);

        try {
            var results = pdfDocumentProcessingService.searchDocuments(query, limit, similarityThreshold);
            return ResponseEntity.ok(new ApiResponse("Document search completed",
                    Map.of("query", query, "results", results, "count", results.size())));
        } catch (Exception e) {
            log.error("Document search failed: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().body(new ApiResponse("Document search failed: " + e.getMessage(), null));
        }
    }

    // === Maintenance Endpoints ===

    @PostMapping("/admin/maintenance/cleanup-expired")
    @PreAuthorize("hasRole('ADMIN')")
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


    @GetMapping("/health")
    public ResponseEntity<ApiResponse> healthCheck() {
        return ResponseEntity.ok(new ApiResponse("Civic Quiz service is healthy",
                Map.of(
                        "service", "civic-quiz",
                        "status", "UP",
                        "timestamp", System.currentTimeMillis()
                )));
    }
}
