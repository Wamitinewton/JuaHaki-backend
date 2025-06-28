package com.juahaki.juahaki.repository.quiz;


import com.juahaki.juahaki.model.quiz.DailyQuiz;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface DailyQuizRepository extends JpaRepository<DailyQuiz, Long> {

    Optional<DailyQuiz> findByQuizDate(LocalDate quizDate);

    @Query("SELECT dq FROM DailyQuiz dq WHERE dq.quizDate = :date AND dq.isActive = true")
    Optional<DailyQuiz> findActiveQuizByDate(@Param("date") LocalDate date);

    @Query("SELECT dq FROM DailyQuiz dq WHERE dq.quizDate = CURRENT_DATE AND dq.isActive = true")
    Optional<DailyQuiz> findTodaysActiveQuiz();

    @Query("SELECT dq FROM DailyQuiz dq WHERE dq.isActive = true AND dq.expiresAt > :now")
    List<DailyQuiz> findActiveQuizzes(@Param("now") LocalDateTime now);

    @Query("SELECT dq FROM DailyQuiz dq WHERE dq.expiresAt <= :now AND dq.isActive = true")
    List<DailyQuiz> findExpiredQuizzes(@Param("now") LocalDateTime now);

    @Query("SELECT dq FROM DailyQuiz dq ORDER BY dq.quizDate DESC")
    List<DailyQuiz> findAllOrderByDateDesc();

    @Query("SELECT dq FROM DailyQuiz dq WHERE dq.quizDate >= :startDate ORDER BY dq.quizDate DESC")
    List<DailyQuiz> findQuizzesFromDate(@Param("startDate") LocalDate startDate);

    boolean existsByQuizDate(LocalDate quizDate);

    @Query("SELECT COUNT(dq) FROM DailyQuiz dq WHERE dq.isActive = true")
    long countActiveQuizzes();

    @Query("SELECT COUNT(dq) FROM DailyQuiz dq WHERE dq.quizDate = :date")
    long countByDate(@Param("date") LocalDate date);
}
