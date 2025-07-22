package com.juahaki.juahaki.infrastructure.config;

import com.juahaki.juahaki.infrastructure.messaging.email.dto.EmailEvent;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.*;
import org.springframework.kafka.listener.ContainerProperties;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.kafka.support.serializer.ErrorHandlingDeserializer;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import org.springframework.kafka.support.serializer.JsonSerializer;
import org.springframework.util.backoff.FixedBackOff;

import java.util.HashMap;
import java.util.Map;

@Configuration
@EnableKafka
@Slf4j
public class KafkaConfig {

    @Value("${spring.kafka.bootstrap-servers}")
    private String bootstrapServers;

    @Value("${app.kafka.email.topic}")
    private String emailTopic;

    @Value("${app.kafka.email.retry-topic}")
    private String emailRetryTopic;

    @Value("${app.kafka.email.dlt-topic}")
    private String emailDltTopic;

    @Value("${app.kafka.consumer.group-id}")
    private String consumerGroupId;

    @Bean
    public ProducerFactory<String, Object> producerFactory() {
        Map<String, Object> configProps = new HashMap<>();
        configProps.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        configProps.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        configProps.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class);

        configProps.put(ProducerConfig.ACKS_CONFIG, "all");
        configProps.put(ProducerConfig.RETRIES_CONFIG, 3);
        configProps.put(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, true);
        configProps.put(ProducerConfig.MAX_IN_FLIGHT_REQUESTS_PER_CONNECTION, 1);

        configProps.put(ProducerConfig.BATCH_SIZE_CONFIG, 16384);
        configProps.put(ProducerConfig.LINGER_MS_CONFIG, 5);
        configProps.put(ProducerConfig.COMPRESSION_TYPE_CONFIG, "snappy");

        return new DefaultKafkaProducerFactory<>(configProps);
    }

    @Bean
    public KafkaTemplate<String, Object> kafkaTemplate() {
        KafkaTemplate<String, Object> template = new KafkaTemplate<>(producerFactory());
        template.setDefaultTopic(emailTopic);
        return template;
    }

    @Bean
    public ConsumerFactory<String, EmailEvent> emailConsumerFactory() {
        Map<String, Object> configProps = new HashMap<>();
        configProps.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        configProps.put(ConsumerConfig.GROUP_ID_CONFIG, consumerGroupId);
        configProps.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        configProps.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, ErrorHandlingDeserializer.class);
        configProps.put(ErrorHandlingDeserializer.VALUE_DESERIALIZER_CLASS, JsonDeserializer.class.getName());
        configProps.put(JsonDeserializer.VALUE_DEFAULT_TYPE, EmailEvent.class.getName());
        configProps.put(JsonDeserializer.TRUSTED_PACKAGES, "com.juahaki.juahaki.dto.email");

        configProps.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        configProps.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false);
        configProps.put(ConsumerConfig.MAX_POLL_RECORDS_CONFIG, 10);
        configProps.put(ConsumerConfig.SESSION_TIMEOUT_MS_CONFIG, 30000);
        configProps.put(ConsumerConfig.HEARTBEAT_INTERVAL_MS_CONFIG, 10000);

        return new DefaultKafkaConsumerFactory<>(configProps);
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, EmailEvent> emailKafkaListenerContainerFactory() {
        ConcurrentKafkaListenerContainerFactory<String, EmailEvent> factory =
                new ConcurrentKafkaListenerContainerFactory<>();

        factory.setConsumerFactory(emailConsumerFactory());
        factory.setConcurrency(3); // Number of consumer threads
        factory.getContainerProperties().setAckMode(ContainerProperties.AckMode.MANUAL_IMMEDIATE);

        // Error handling with retry mechanism
        DefaultErrorHandler errorHandler = new DefaultErrorHandler(
                new FixedBackOff(5000L, 3L)
        );

        errorHandler.addNotRetryableExceptions(IllegalAccessException.class);
        factory.setCommonErrorHandler(errorHandler);

        return factory;
    }

    @Bean
    public NewTopic emailTopic() {
        return TopicBuilder.name(emailTopic)
                .partitions(3)
                .replicas(1)
                .config("retention.ms", "604800000") // 7 days
                .config("compression.type", "snappy")
                .build();
    }

    @Bean
    public NewTopic emailRetryTopic() {
        return TopicBuilder.name(emailRetryTopic)
                .partitions(3)
                .replicas(1)
                .config("retention.ms", "86400000") // 1 day
                .build();
    }

    @Bean
    public NewTopic emailDltTopic() {
        return TopicBuilder.name(emailDltTopic)
                .partitions(1)
                .replicas(1)
                .config("retention.ms", "2592000000") // 30 days
                .build();
    }
}
