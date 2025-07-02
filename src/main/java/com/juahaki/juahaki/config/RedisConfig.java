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

        Map<String, RedisCacheConfiguration> cacheConfigurations = new HashMap<>();
        
        cacheConfigurations.put("dailyQuiz", defaultCacheConfig
                .entryTtl(Duration.ofHours(24))
                .prefixCacheNameWith("quiz:daily:"));
        
        cacheConfigurations.put("quizInfo", defaultCacheConfig
                .entryTtl(Duration.ofHours(2))
                .prefixCacheNameWith("quiz:info:"));
        
        cacheConfigurations.put("generatedQuiz", defaultCacheConfig
                .entryTtl(Duration.ofHours(24))
                .prefixCacheNameWith("quiz:generated:"));

        cacheConfigurations.put("quizDetails", defaultCacheConfig
                .entryTtl(Duration.ofHours(24))
                .prefixCacheNameWith("quiz:details:"));
        
        cacheConfigurations.put("quizQuestions", defaultCacheConfig
                .entryTtl(Duration.ofHours(24))
                .prefixCacheNameWith("quiz:questions:"));
        
        cacheConfigurations.put("leaderboard", defaultCacheConfig
                .entryTtl(Duration.ofMinutes(30))
                .prefixCacheNameWith("quiz:leaderboard:"));
        
        cacheConfigurations.put("quizStats", defaultCacheConfig
                .entryTtl(Duration.ofHours(1))
                .prefixCacheNameWith("quiz:stats:"));

        cacheConfigurations.put("userQuizHistory", defaultCacheConfig
                .entryTtl(Duration.ofMinutes(30))
                .prefixCacheNameWith("quiz:history:metadata:"));

        cacheConfigurations.put("quizAIContext", defaultCacheConfig
                .entryTtl(Duration.ofHours(6))
                .prefixCacheNameWith("quiz:ai:context:"));

        cacheConfigurations.put("quizQuality", defaultCacheConfig
                .entryTtl(Duration.ofHours(12))
                .prefixCacheNameWith("quiz:quality:"));

        return RedisCacheManager.builder(connectionFactory)
                .cacheDefaults(defaultCacheConfig)
                .withInitialCacheConfigurations(cacheConfigurations)
                .build();
    }

    private ObjectMapper createQuizObjectMapper() {
        ObjectMapper objectMapper = new ObjectMapper();
        
        objectMapper.registerModule(new JavaTimeModule());
        
        objectMapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);

        objectMapper.activateDefaultTyping(
                LaissezFaireSubTypeValidator.instance,
                ObjectMapper.DefaultTyping.NON_FINAL,
                JsonTypeInfo.As.PROPERTY
        );

        return objectMapper;
    }
}