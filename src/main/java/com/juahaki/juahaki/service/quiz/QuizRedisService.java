package com.juahaki.juahaki.service.quiz;

import com.juahaki.juahaki.dto.quiz.civic.CivicQuizSessionData;
import lombok.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
@Slf4j
public class QuizRedisService implements IQuizRedisService {

    private final RedisTemplate<String, Object> redisTemplate;

    private static final String CIVIC_QUIZ_SESSION_PREFIX = "civic_quiz_session:";
    private static final String GENERAL_QUIZ_SESSION_PREFIX = "quiz_session:";

    @Override
    public boolean storeSession(String sessionId, Object sessionData, long timeout, TimeUnit timeUnit) {
        try {
            String redisKey = buildRedisKey(sessionId);
            redisTemplate.opsForValue().set(redisKey, sessionData, timeout, timeUnit);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> Optional<T> getSession(String sessionId, Class<T> clazz) {
        try {
            String redisKey = buildRedisKey(sessionId);
            Object sessionData = redisTemplate.opsForValue().get(redisKey);
            if (sessionData != null && clazz.isInstance(sessionData)) {
                return Optional.of((T) sessionData);
            }

            return Optional.empty();
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    @Override
    public boolean updateSession(String sessionId, Object sessionData, long timeout, TimeUnit timeUnit) {
        try {
            String redisKey = buildRedisKey(sessionId);

            if (!redisTemplate.hasKey(redisKey)) {
                return false;
            }
            redisTemplate.opsForValue().set(redisKey, sessionData, timeout, timeUnit);
            return true;
        } catch (Exception e) {
            log.error("Failed to update session for id: {}", sessionId);
            return false;
        }
    }

    @Override
    public boolean removeSession(String sessionId) {
        try {
            String redisKey = buildRedisKey(sessionId);
            Boolean deleted = redisTemplate.delete(redisKey);

            return Boolean.TRUE.equals(deleted);
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public boolean sessionExists(String sessionId) {
        try {
            String redisKey = buildRedisKey(sessionId);
            Boolean exists = redisTemplate.hasKey(redisKey);
            return Boolean.TRUE.equals(exists);
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public boolean extendSessionTimeout(String sessionId, long timeout, TimeUnit timeUnit) {
        try {
            String redisKey = buildRedisKey(sessionId);
            if (!redisTemplate.hasKey(redisKey)) {
                log.warn("Attempted to extend timeout for non-existent session: {}", sessionId);
                return false;
            }

            Boolean extended = redisTemplate.expire(redisKey, timeout, timeUnit);

            return Boolean.TRUE.equals(extended);
        } catch (Exception e) {
            log.error("Failed to extend timeout for quiz session: {}", sessionId);
            return false;
        }
    }

    @Override
    public long getSessionTTL(String sessionId) {
        try {
            String redisKey = buildRedisKey(sessionId);
            return redisTemplate.getExpire(redisKey, TimeUnit.SECONDS);
        } catch (Exception e) {
            return -2;
        }
    }

    @Override
    public boolean storeCivicQuizSession(String sessionId, CivicQuizSessionData sessionData, long timeout, TimeUnit timeUnit) {
        try {
            String redisKey = CIVIC_QUIZ_SESSION_PREFIX + sessionId;
            redisTemplate.opsForValue().set(redisKey, sessionData, timeout, timeUnit);
            log.debug("Successfully stored civic quiz session: {}", sessionId);
            return true;
        } catch (Exception e) {
            log.error("Failed to store civic quiz session {}: {}", sessionId, e.getMessage(), e);
            return false;
        }
    }

    @Override
    public Optional<CivicQuizSessionData> getCivicQuizSession(String sessionId) {
        try {
            String redisKey = CIVIC_QUIZ_SESSION_PREFIX + sessionId;
            Object sessionData = redisTemplate.opsForValue().get(redisKey);

            if (sessionData instanceof CivicQuizSessionData) {
                log.debug("Successfully retrieved civic quiz session: {}", sessionId);
                return Optional.of((CivicQuizSessionData) sessionData);
            }

            log.debug("Civic quiz session not found: {}", sessionId);
            return Optional.empty();
        } catch (Exception e) {
            log.error("Failed to retrieve civic quiz session {}: {}", sessionId, e.getMessage(), e);
            return Optional.empty();
        }
    }

    @Override
    public boolean updateCivicQuizSession(String sessionId, CivicQuizSessionData sessionData, long timeout, TimeUnit timeUnit) {
        try {
            String redisKey = CIVIC_QUIZ_SESSION_PREFIX + sessionId;
            redisTemplate.opsForValue().set(redisKey, sessionData, timeout, timeUnit);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public boolean removeCivicQuizSession(String sessionId) {
        try {
            String redisKey = CIVIC_QUIZ_SESSION_PREFIX + sessionId;
            Boolean deleted = redisTemplate.delete(redisKey);
            log.debug("Civic quiz session removal result for {}: {}", sessionId, deleted);
            return Boolean.TRUE.equals(deleted);
        } catch (Exception e) {
            return false;
        }
    }

    private String buildRedisKey(String sessionId) {
        if (sessionId.startsWith("civic_")) {
            return CIVIC_QUIZ_SESSION_PREFIX + sessionId;
        }
        return GENERAL_QUIZ_SESSION_PREFIX + sessionId;
    }
}