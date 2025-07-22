package com.juahaki.juahaki.core.quiz.repository;



import com.juahaki.juahaki.core.quiz.model.UserAnswer;
import com.juahaki.juahaki.core.quiz.model.UserQuizAttempt;

import java.util.List;

public interface UserAnswerCustomRepository {
    List<UserAnswer> findByAttemptOrderByQuestionNumber(UserQuizAttempt attempt);
}
