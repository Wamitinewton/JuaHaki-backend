package com.juahaki.juahaki.repository.quiz;

import com.juahaki.juahaki.model.quiz.CivicQuestion;
import com.juahaki.juahaki.model.quiz.DailyQuiz;

import java.util.Optional;

public interface CivicQuestionCustomRepository {
    Optional<CivicQuestion> findByQuizAndNumber(DailyQuiz dailyQuiz, int questionNumber);
}