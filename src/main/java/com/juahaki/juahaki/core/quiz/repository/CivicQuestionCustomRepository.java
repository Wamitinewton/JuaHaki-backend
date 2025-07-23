package com.juahaki.juahaki.core.quiz.repository;


import com.juahaki.juahaki.core.quiz.model.CivicQuestion;
import com.juahaki.juahaki.core.quiz.model.DailyQuiz;

import java.util.Optional;

public interface CivicQuestionCustomRepository {
    Optional<CivicQuestion> findByQuizAndNumber(DailyQuiz dailyQuiz, int questionNumber);
}