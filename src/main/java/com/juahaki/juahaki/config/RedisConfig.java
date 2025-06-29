package com.juahaki.juahaki.config;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.jsontype.impl.LaissezFaireSubTypeValidator;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceClientConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

@Configuration
@EnableCaching
@Slf4j
public class RedisConfig {

    @Value("${spring.data.redis.host}")
    private String redisHost;

    @Value("${spring.data.redis.port}")
    private int redisPort;

    @Value("${spring.data.redis.password}")
    private String redisPassword;

    @Value("${spring.data.redis.ssl.enabled}")
    private boolean sslEnabled;

    @Bean
    @Primary
    public RedisConnectionFactory redisConnectionFactory() {
        log.info("Configuring Redis connection to {}:{} with SSL: {}", redisHost, redisPort, sslEnabled);

        RedisStandaloneConfiguration redisConfig = new RedisStandaloneConfiguration();
        redisConfig.setHostName(redisHost);
        redisConfig.setPort(redisPort);

        if (redisPassword != null && !redisPassword.trim().isEmpty()) {
            redisConfig.setPassword(redisPassword);
        }

        LettuceClientConfiguration.LettuceClientConfigurationBuilder clientConfigurationBuilder = LettuceClientConfiguration
                .builder()
                .commandTimeout(Duration.ofSeconds(5))
                .shutdownTimeout(Duration.ofMillis(100));

        if (sslEnabled) {
            clientConfigurationBuilder.useSsl();
        }

        LettuceClientConfiguration clientConfiguration = clientConfigurationBuilder.build();
        return new LettuceConnectionFactory(redisConfig, clientConfiguration);
    }

    @Bean
    @Primary
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);

        ObjectMapper objectMapper = createQuizObjectMapper();
        Jackson2JsonRedisSerializer<Object> jsonRedisSerializer = new Jackson2JsonRedisSerializer<>(objectMapper, Object.class);
        StringRedisSerializer stringRedisSerializer = new StringRedisSerializer();

        template.setKeySerializer(stringRedisSerializer);
        template.setHashKeySerializer(stringRedisSerializer);
        template.setValueSerializer(jsonRedisSerializer);
        template.setHashValueSerializer(jsonRedisSerializer);
        template.setDefaultSerializer(jsonRedisSerializer);
        template.afterPropertiesSet();

        return template;
    }

    @Bean
    @Primary
    public CacheManager cacheManager(RedisConnectionFactory connectionFactory) {
        log.info("Configuring Redis Cache Manager with Quiz-specific configurations");

        ObjectMapper objectMapper = createQuizObjectMapper();
        GenericJackson2JsonRedisSerializer serializer = new GenericJackson2JsonRedisSerializer(objectMapper);

        RedisCacheConfiguration defaultCacheConfig = RedisCacheConfiguration.defaultCacheConfig()
                .serializeKeysWith(org.springframework.data.redis.serializer.RedisSerializationContext.SerializationPair
                        .fromSerializer(new StringRedisSerializer()))
                .serializeValuesWith(org.springframework.data.redis.serializer.RedisSerializationContext.SerializationPair
                        .fromSerializer(serializer))
                .entryTtl(Duration.ofHours(1))
                .disableCachingNullValues();

        // Define cache-specific configurations for quiz system
        Map<String, RedisCacheConfiguration> cacheConfigurations = new HashMap<>();
        
        // Daily Quiz cache - cache for 24 hours (until next day)
        cacheConfigurations.put("dailyQuiz", defaultCacheConfig
                .entryTtl(Duration.ofHours(24))
                .prefixCacheNameWith("quiz:daily:"));
        
        // Quiz info cache - cache for 2 hours
        cacheConfigurations.put("quizInfo", defaultCacheConfig
                .entryTtl(Duration.ofHours(2))
                .prefixCacheNameWith("quiz:info:"));
        
        // Generated quiz content - cache for 24 hours (same as daily quiz)
        cacheConfigurations.put("generatedQuiz", defaultCacheConfig
                .entryTtl(Duration.ofHours(24))
                .prefixCacheNameWith("quiz:generated:"));
        
        // Quiz questions cache - cache for 24 hours
        cacheConfigurations.put("quizQuestions", defaultCacheConfig
                .entryTtl(Duration.ofHours(24))
                .prefixCacheNameWith("quiz:questions:"));
        
        // Leaderboard cache - cache for 30 minutes (frequently updated)
        cacheConfigurations.put("leaderboard", defaultCacheConfig
                .entryTtl(Duration.ofMinutes(30))
                .prefixCacheNameWith("quiz:leaderboard:"));
        
        // Quiz statistics cache - cache for 1 hour
        cacheConfigurations.put("quizStats", defaultCacheConfig
                .entryTtl(Duration.ofHours(1))
                .prefixCacheNameWith("quiz:stats:"));
        
        // User quiz history cache - cache for 30 minutes
        cacheConfigurations.put("userQuizHistory", defaultCacheConfig
                .entryTtl(Duration.ofMinutes(30))
                .prefixCacheNameWith("quiz:history:"));

        // Quiz AI context cache - cache for 6 hours
        cacheConfigurations.put("quizAIContext", defaultCacheConfig
                .entryTtl(Duration.ofHours(6))
                .prefixCacheNameWith("quiz:ai:context:"));

        // Quiz quality analysis cache - cache for 12 hours
        cacheConfigurations.put("quizQuality", defaultCacheConfig
                .entryTtl(Duration.ofHours(12))
                .prefixCacheNameWith("quiz:quality:"));

        return RedisCacheManager.builder(connectionFactory)
                .cacheDefaults(defaultCacheConfig)
                .withInitialCacheConfigurations(cacheConfigurations)
                .build();
    }

    /**
     * Creates ObjectMapper specifically configured for quiz data serialization
     */
    private ObjectMapper createQuizObjectMapper() {
        ObjectMapper objectMapper = new ObjectMapper();
        
        // Register JavaTimeModule for LocalDate, LocalDateTime serialization
        objectMapper.registerModule(new JavaTimeModule());
        
        // Include non-null values only
        objectMapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);

        // Enable type information for proper deserialization of complex objects
        objectMapper.activateDefaultTyping(
                LaissezFaireSubTypeValidator.instance,
                ObjectMapper.DefaultTyping.NON_FINAL,
                JsonTypeInfo.As.PROPERTY
        );

        return objectMapper;
    }
}