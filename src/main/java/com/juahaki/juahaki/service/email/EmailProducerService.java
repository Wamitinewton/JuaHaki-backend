package com.juahaki.juahaki.service.email;

import com.juahaki.juahaki.dto.email.EmailEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailProducerService {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Value("${app.kafka.email.topic}")
    private String emailTopic;

    @Value("${app.kafka.email.retry-topic}")
    private String emailRetryTopic;

    /**
     * Send email event to kafka topic
     */
    public void sendEmailEvent(EmailEvent emailEvent) {
        try {
            String key = generateMessageKey(emailEvent);

            CompletableFuture<SendResult<String, Object>> future =
                    kafkaTemplate.send(emailTopic, key, emailEvent);

            future.whenComplete((result, ex) -> {
                if (ex == null) {
                    log.info("Email event sent successfully: eventId={}, type={}, recipient={}, offset={}",
                            emailEvent.getEventId(),
                            emailEvent.getEmailType(),
                            emailEvent.getRecipient(),
                            result.getRecordMetadata().offset());
                } else {
                    log.error("Failed to send email event: eventId={}, type={}, recipient={}, error={}",
                            emailEvent.getEventId(),
                            emailEvent.getEmailType(),
                            emailEvent.getRecipient(),
                            ex.getMessage(), ex);
                }
            });
        } catch (Exception e) {
            log.error("Error sending email event to Kafka: eventId={}, error={}",
                    emailEvent.getEventId(), e.getMessage(), e);
            throw new RuntimeException("Failed to queue email for processing", e);
        }
    }

    /**
     * Send email event to retry topic
     */
    public void sendToRetryTopic(EmailEvent emailEvent) {
        try {
            String key = generateMessageKey(emailEvent);
            emailEvent.incrementRetryCount();

            CompletableFuture<SendResult<String, Object>> future =
                    kafkaTemplate.send(emailRetryTopic, key, emailEvent);

            future.whenComplete((result, ex) -> {
                if (ex == null) {
                    log.info("Email event sent to retry topic: eventId={}, retryCount={}, offset={}",
                            emailEvent.getEventId(),
                            emailEvent.getRetryCount(),
                            result.getRecordMetadata().offset());
                } else {
                    log.error("Failed to send email event to retry topic: eventId={}, error={}",
                            emailEvent.getEventId(), ex.getMessage(), ex);
                }
            });
        } catch (Exception e) {
            log.error("Error sending email event to retry topic: eventId={}, error={}",
                    emailEvent.getEventId(), e.getMessage(), e);
        }
    }

    /**
     * Generate message key for partitioning
     * Using recipient email to ensure emails for same user go to same partition
     */
    private String generateMessageKey(EmailEvent emailEvent) {
        return emailEvent.getRecipient() + "_" + emailEvent.getEmailType();
    }
}
