package com.juahaki.juahaki.service.email;

import com.juahaki.juahaki.dto.email.EmailEvent;
import com.juahaki.juahaki.dto.email.EmailRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailConsumerService {

    private final AccountManagementEmailService accountManagementEmailService;
    private final EmailSender emailSender;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Value("${app.kafka.email.retry-topic}")
    private String emailRetryTopic;

    @Value("${app.kafka.email.dlt-topic}")
    private String emailDltTopic;

    @Value("${app.email.max-retry-attempts:3}")
    private int maxRetryAttempts;


    @KafkaListener(
            topics = "${app.kafka.email.topic}",
            groupId = "${app.kafka.consumer.group-id}",
            containerFactory = "emailKafkaListenerContainerFactory"
    )
    public void handleEmailEvent(
            @Payload EmailEvent emailEvent,
            @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
            @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
            @Header(KafkaHeaders.OFFSET) long offset,
            Acknowledgment acknowledgment) {

        log.info("Processing email event: eventId={}, type={}, recipient={}, topic={}, partition={}, offset={}",
                emailEvent.getEventId(),
                emailEvent.getEmailType(),
                emailEvent.getRecipient(),
                topic, partition, offset);

        try {
            processEmailEvent(emailEvent);
            acknowledgment.acknowledge();

            log.info("Email event processed successfully: eventId={}, type={}, recipient={}",
                    emailEvent.getEventId(),
                    emailEvent.getEmailType(),
                    emailEvent.getRecipient());

        } catch (Exception e) {
            log.error("Error processing email event: eventId={}, type={}, recipient={}, error={}",
                    emailEvent.getEventId(),
                    emailEvent.getEmailType(),
                    emailEvent.getRecipient(),
                    e.getMessage(), e);

            handleEmailFailure(emailEvent, e);
            acknowledgment.acknowledge();
        }
    }

    /**
     * Retry topic consumer
     */
    @KafkaListener(
            topics = "${app.kafka.email.retry-topic}",
            groupId = "${app.kafka.consumer.group-id}-retry",
            containerFactory = "emailKafkaListenerContainerFactory"
    )
    public void handleRetryEmailEvent(
            @Payload EmailEvent emailEvent,
            @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
            Acknowledgment acknowledgment) {

        log.info("Processing retry email event: eventId={}, type={}, recipient={}, retryCount={}",
                emailEvent.getEventId(),
                emailEvent.getEmailType(),
                emailEvent.getRecipient(),
                emailEvent.getRetryCount());

        try {
            processEmailEvent(emailEvent);
            acknowledgment.acknowledge();

            log.info("Retry email event processed successfully: eventId={}, retryCount={}",
                    emailEvent.getEventId(), emailEvent.getRetryCount());

        } catch (Exception e) {
            log.error("Error processing retry email event: eventId={}, retryCount={}, error={}",
                    emailEvent.getEventId(), emailEvent.getRetryCount(), e.getMessage(), e);

            handleEmailFailure(emailEvent, e);
            acknowledgment.acknowledge();
        }
    }


    private void processEmailEvent(EmailEvent emailEvent) {
        try {
            Map<String, Object> enrichedVariables = templateVariables(emailEvent.getTemplateVariables());

            EmailRequest emailRequest = EmailRequest.builder()
                    .to(emailEvent.getRecipient())
                    .subject(emailEvent.getSubject())
                    .templateName(emailEvent.getTemplateName())
                    .variables(enrichedVariables)
                    .isHtml(emailEvent.isHtml())
                    .build();

            emailSender.sendEmail(emailRequest);

        } catch (Exception e) {
            log.error("Failed to process email event: eventId={}, error={}",
                    emailEvent.getEventId(), e.getMessage());
            throw e;
        }
    }


    private void handleEmailFailure(EmailEvent emailEvent, Exception error) {
        if (emailEvent.getRetryCount() < maxRetryAttempts) {
            log.info("Sending email event to retry topic: eventId={}, retryCount={}",
                    emailEvent.getEventId(), emailEvent.getRetryCount() + 1);

            try {
                emailEvent.incrementRetryCount();
                kafkaTemplate.send(emailRetryTopic, emailEvent);
            } catch (Exception e) {
                log.error("Failed to send email event to retry topic: eventId={}, error={}",
                        emailEvent.getEventId(), e.getMessage(), e);
                sendToDeadLetterTopic(emailEvent, error);
            }
        } else {
            log.warn("Max retry attempts exceeded for email event: eventId={}, retryCount={}",
                    emailEvent.getEventId(), emailEvent.getRetryCount());

            sendToDeadLetterTopic(emailEvent, error);
        }
    }


    private void sendToDeadLetterTopic(EmailEvent emailEvent, Exception error) {
        try {
            Map<String, Object> errorInfo = new HashMap<>();
            errorInfo.put("error", error.getMessage());
            errorInfo.put("failedAt", LocalDateTime.now().toString());
            errorInfo.put("finalRetryCount", emailEvent.getRetryCount());

            EmailEvent failedEvent = EmailEvent.builder()
                    .eventId(emailEvent.getEventId())
                    .emailType(emailEvent.getEmailType())
                    .recipient(emailEvent.getRecipient())
                    .subject(emailEvent.getSubject())
                    .templateName(emailEvent.getTemplateName())
                    .templateVariables(errorInfo)
                    .isHtml(emailEvent.isHtml())
                    .userId(emailEvent.getUserId())
                    .retryCount(emailEvent.getRetryCount())
                    .createdAt(emailEvent.getCreatedAt())
                    .build();

            kafkaTemplate.send(emailDltTopic, failedEvent);

            log.error("Email event sent to dead letter topic: eventId={}, type={}, recipient={}, finalError={}",
                    emailEvent.getEventId(),
                    emailEvent.getEmailType(),
                    emailEvent.getRecipient(),
                    error.getMessage());

        } catch (Exception e) {
            log.error("Failed to send email event to dead letter topic: eventId={}, error={}",
                    emailEvent.getEventId(), e.getMessage(), e);
        }
    }


    private Map<String, Object> templateVariables(Map<String, Object> originalVariables) {
        Map<String, Object> templateVariables = new HashMap<>();

        if (originalVariables != null) {
            templateVariables.putAll(originalVariables);
        }

        templateVariables.putIfAbsent("supportEmail", "support@juahaki.com");
        templateVariables.putIfAbsent("supportPhone", "+254-700-000-000");
        templateVariables.putIfAbsent("companyName", "JuaHaki Civic Educator");
        templateVariables.putIfAbsent("currentYear", java.time.Year.now().getValue());
        templateVariables.putIfAbsent("baseUrl", "https://juahaki.com");
        templateVariables.putIfAbsent("loginUrl", "https://juahaki.com/login");
        templateVariables.putIfAbsent("contactUrl", "https://juahaki.com/contact");

        return templateVariables;
    }
}
