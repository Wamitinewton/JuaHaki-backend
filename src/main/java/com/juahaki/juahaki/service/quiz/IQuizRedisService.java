package com.juahaki.juahaki.service.quiz;


import com.juahaki.juahaki.dto.quiz.civic.CivicQuizSessionData;

import java.util.Optional;
import java.util.concurrent.TimeUnit;

public interface IQuizRedisService {

    /**
     * Store quiz session data in Redis
     *
     * @param sessionId   Unique session identifier
     * @param sessionData Session data to store
     * @param timeout     Timeout duration
     * @param timeUnit    Time unit for timeout
     * @return true if stored successfully, false otherwise
     */
    boolean storeSession(String sessionId, Object sessionData, long timeout, TimeUnit timeUnit);

    /**
     * Retrieve quiz session data from Redis
     *
     * @param sessionId Session identifier
     * @param clazz     Expected class type of session data
     * @return Optional containing session data if found
     */
    <T> Optional<T> getSession(String sessionId, Class<T> clazz);

    /**
     * Update existing quiz session data in Redis
     *
     * @param sessionId   Session identifier
     * @param sessionData Updated session data
     * @param timeout     New timeout duration
     * @param timeUnit    Time unit for timeout
     * @return true if updated successfully, false otherwise
     */
    boolean updateSession(String sessionId, Object sessionData, long timeout, TimeUnit timeUnit);

    /**
     * Remove quiz session from Redis
     *
     * @param sessionId Session identifier
     * @return true if removed successfully, false otherwise
     */
    boolean removeSession(String sessionId);

    /**
     * Check if a session exists in Redis
     *
     * @param sessionId Session identifier
     * @return true if session exists, false otherwise
     */
    boolean sessionExists(String sessionId);

    /**
     * Extend session timeout
     *
     * @param sessionId Session identifier
     * @param timeout   New timeout duration
     * @param timeUnit  Time unit for timeout
     * @return true if timeout extended successfully, false otherwise
     */
    boolean extendSessionTimeout(String sessionId, long timeout, TimeUnit timeUnit);

    /**
     * Get session time-to-live in seconds
     *
     * @param sessionId Session identifier
     * @return TTL in seconds, -1 if key doesn't exist, -2 if key exists but has no TTL
     */
    long getSessionTTL(String sessionId);

    /**
     * Store civic quiz session data in Redis
     *
     * @param sessionId   Session identifier
     * @param sessionData Civic quiz session data
     * @param timeout     Timeout duration
     * @param timeUnit    Time unit for timeout
     * @return true if stored successfully, false otherwise
     */
    boolean storeCivicQuizSession(String sessionId, CivicQuizSessionData sessionData, long timeout, TimeUnit timeUnit);

    /**
     * Retrieve civic quiz session data from Redis
     *
     * @param sessionId Session identifier
     * @return Optional containing civic quiz session data if found
     */
    Optional<CivicQuizSessionData> getCivicQuizSession(String sessionId);

    /**
     * Update civic quiz session data in Redis
     *
     * @param sessionId   Session identifier
     * @param sessionData Updated civic quiz session data
     * @param timeout     New timeout duration
     * @param timeUnit    Time unit for timeout
     * @return true if updated successfully, false otherwise
     */
    boolean updateCivicQuizSession(String sessionId, CivicQuizSessionData sessionData, long timeout, TimeUnit timeUnit);

    /**
     * Remove civic quiz session from Redis
     *
     * @param sessionId Session identifier
     * @return true if removed successfully, false otherwise
     */
    boolean removeCivicQuizSession(String sessionId);
}