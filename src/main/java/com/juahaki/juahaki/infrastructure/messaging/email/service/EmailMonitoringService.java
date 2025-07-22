package com.juahaki.juahaki.infrastructure.messaging.email.service;

import com.juahaki.juahaki.infrastructure.messaging.email.dto.EmailEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailMonitoringService {

    private final AtomicLong totalEmailsProcessed = new AtomicLong(0);
    private final AtomicLong totalEmailsFailed = new AtomicLong(0);
    private final ConcurrentHashMap<String, Long> emailTypeCounters = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, EmailEvent> deadLetterEvents = new ConcurrentHashMap<>();

    @Value("${app.kafka.email.dlt-topic}")
    private String emailDltTopic;

    /**
     * Monitor dead letter topic for failed emails
     */
    @KafkaListener(
            topics = "${app.kafka.email.dlt-topic}",
            groupId = "${app.kafka.consumer.group-id}-monitor"
    )
    public void monitorDeadLetterTopic(
            @Payload EmailEvent emailEvent,
            @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
            @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
            @Header(KafkaHeaders.OFFSET) long offset,
            Acknowledgment acknowledgment) {

        log.error("Email event in dead letter topic: eventId={}, type={}, recipient={}, retryCount={}",
                emailEvent.getEventId(),
                emailEvent.getEmailType(),
                emailEvent.getRecipient(),
                emailEvent.getRetryCount());

        deadLetterEvents.put(emailEvent.getEventId(), emailEvent);
        totalEmailsFailed.incrementAndGet();
        acknowledgment.acknowledge();
    }


    public void recordEmailProcessed(String emailType) {
        totalEmailsProcessed.incrementAndGet();
        emailTypeCounters.merge(emailType, 1L, Long::sum);

        log.debug("Email processed: type={}, total={}", emailType, totalEmailsProcessed.get());
    }

    /**
     * Get email processing statistics
     */
    public EmailStatistics getEmailStatistics() {
        return EmailStatistics.builder()
                .totalProcessed(totalEmailsProcessed.get())
                .totalFailed(totalEmailsFailed.get())
                .typeCounters(new ConcurrentHashMap<>(emailTypeCounters))
                .deadLetterCount(deadLetterEvents.size())
                .lastUpdated(LocalDateTime.now())
                .build();
    }


    public ConcurrentHashMap<String, EmailEvent> getFailedEmails() {
        return new ConcurrentHashMap<>(deadLetterEvents);
    }


    public void clearStatistics() {
        totalEmailsProcessed.set(0);
        totalEmailsFailed.set(0);
        emailTypeCounters.clear();
        deadLetterEvents.clear();

        log.info("Email statistics cleared by administrator");
    }

    @lombok.Data
    @lombok.Builder
    @lombok.AllArgsConstructor
    @lombok.NoArgsConstructor
    public static class EmailStatistics {
        private long totalProcessed;
        private long totalFailed;
        private ConcurrentHashMap<String, Long> typeCounters;
        private int deadLetterCount;
        private LocalDateTime lastUpdated;

        public double getSuccessRate() {
            long total = totalProcessed + totalFailed;
            return total > 0 ? (double) totalProcessed / total * 100 : 0.0;
        }
    }
}
