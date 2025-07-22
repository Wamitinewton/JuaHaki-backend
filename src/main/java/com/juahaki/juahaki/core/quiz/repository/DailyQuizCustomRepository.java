package com.juahaki.juahaki.core.quiz.repository;



import com.juahaki.juahaki.core.quiz.model.DailyQuiz;

import java.time.LocalDate;
import java.util.Optional;

public interface DailyQuizCustomRepository {
    Optional<DailyQuiz> findActiveQuizByDate(LocalDate date);
    Optional<DailyQuiz> findTodaysActiveQuiz();
}