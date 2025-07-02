package com.juahaki.juahaki.controller.admin.email;

import com.juahaki.juahaki.dto.email.EmailEvent;
import com.juahaki.juahaki.response.ApiResponse;
import com.juahaki.juahaki.service.email.EmailMonitoringService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@RestController
@RequestMapping("${api.prefix}/admin/email")
@RequiredArgsConstructor
@Slf4j
public class AdminEmailMonitoringController {

    private final EmailMonitoringService emailMonitoringService;


    @GetMapping("/statistics")
    public ResponseEntity<ApiResponse> getEmailStatistics() {
        log.info("Admin requesting email statistics");

        EmailMonitoringService.EmailStatistics statistics = emailMonitoringService.getEmailStatistics();

        return ResponseEntity.ok(new ApiResponse("Email statistics retrieved successfully", statistics));
    }


    @GetMapping("/failed-emails")
    public ResponseEntity<ApiResponse> getFailedEmails() {
        log.info("Admin requesting failed emails");

        ConcurrentHashMap<String, EmailEvent> failedEmails = emailMonitoringService.getFailedEmails();

        return ResponseEntity.ok(new ApiResponse("Failed emails retrieved successfully",
                Map.of(
                        "count", failedEmails.size(),
                        "emails", failedEmails
                )));
    }


    @PostMapping("/clear-statistics")
    public ResponseEntity<ApiResponse> clearStatistics() {
        log.info("Admin clearing email statistics");

        emailMonitoringService.clearStatistics();

        return ResponseEntity.ok(new ApiResponse("Email statistics cleared successfully", null));
    }


    @GetMapping("/health")
    public ResponseEntity<ApiResponse> getEmailServiceHealth() {
        log.info("Admin requesting email service health status");

        try {
            EmailMonitoringService.EmailStatistics statistics = emailMonitoringService.getEmailStatistics();

            Map<String, Object> healthStatus = Map.of(
                    "status", statistics.getSuccessRate() > 95.0 ? "HEALTHY" : "WARNING",
                    "successRate", statistics.getSuccessRate(),
                    "totalProcessed", statistics.getTotalProcessed(),
                    "totalFailed", statistics.getTotalFailed(),
                    "deadLetterCount", statistics.getDeadLetterCount(),
                    "lastUpdated", statistics.getLastUpdated()
            );

            return ResponseEntity.ok(new ApiResponse("Email service health status retrieved", healthStatus));

        } catch (Exception e) {
            log.error("Error retrieving email service health: {}", e.getMessage(), e);
            return ResponseEntity.badRequest()
                    .body(new ApiResponse("Failed to retrieve email service health", null));
        }
    }
}
