package com.juahaki.juahaki.core.quiz.repository;


import com.juahaki.juahaki.core.quiz.model.DailyQuiz;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.Optional;

@Repository
public interface DailyQuizRepository extends JpaRepository<DailyQuiz, Long> {

    Optional<DailyQuiz> findByQuizDate(LocalDate quizDate);

    @Query("SELECT dq FROM DailyQuiz dq WHERE dq.quizDate = :date AND dq.isActive = true")
    Optional<DailyQuiz> findActiveQuizByDate(@Param("date") LocalDate date);

    @Query("SELECT dq FROM DailyQuiz dq WHERE dq.quizDate = CURRENT_DATE AND dq.isActive = true")
    Optional<DailyQuiz> findTodaysActiveQuiz();

    boolean existsByQuizDate(LocalDate quizDate);

}
