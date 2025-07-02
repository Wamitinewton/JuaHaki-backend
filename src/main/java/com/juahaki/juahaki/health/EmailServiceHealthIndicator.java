package com.juahaki.juahaki.health;

import com.juahaki.juahaki.service.email.EmailMonitoringService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component("emailService")
@RequiredArgsConstructor
@Slf4j
public class EmailServiceHealthIndicator implements HealthIndicator {

    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final EmailMonitoringService emailMonitoringService;

    @Override
    public Health health() {
        try {
            kafkaTemplate.metrics();

            EmailMonitoringService.EmailStatistics stats = emailMonitoringService.getEmailStatistics();

            Health.Builder healthBuilder = Health.up()
                    .withDetail("kafka", "connected")
                    .withDetail("totalEmailsProcessed", stats.getTotalProcessed())
                    .withDetail("totalEmailsFailed", stats.getTotalFailed())
                    .withDetail("successRate", String.format("%.2f%%", stats.getSuccessRate()))
                    .withDetail("deadLetterCount", stats.getDeadLetterCount());

            if (stats.getSuccessRate() < 95.0 && stats.getTotalProcessed() > 10) {
                healthBuilder.withDetail("warning", "Email success rate below 95%");
            }

            return healthBuilder.build();

        } catch (Exception e) {
            log.error("Email service health check failed: {}", e.getMessage());
            return Health.down()
                    .withDetail("kafka", "disconnected")
                    .withDetail("error", e.getMessage())
                    .build();
        }
    }
}
