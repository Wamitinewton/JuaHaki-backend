package com.juahaki.juahaki.repository.quiz;


import com.juahaki.juahaki.model.quiz.UserAnswer;
import com.juahaki.juahaki.model.quiz.UserQuizAttempt;

import java.util.List;

public interface UserAnswerCustomRepository {
    List<UserAnswer> findByAttemptOrderByQuestionNumber(UserQuizAttempt attempt);
}
