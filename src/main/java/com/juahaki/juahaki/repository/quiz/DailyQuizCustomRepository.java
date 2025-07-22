package com.juahaki.juahaki.repository.quiz;


import com.juahaki.juahaki.model.quiz.DailyQuiz;

import java.time.LocalDate;
import java.util.Optional;

public interface DailyQuizCustomRepository {
    Optional<DailyQuiz> findActiveQuizByDate(LocalDate date);
    Optional<DailyQuiz> findTodaysActiveQuiz();
}