package com.juahaki.juahaki.core.quiz.repository;

import com.juahaki.juahaki.core.quiz.model.CivicQuestion;
import com.juahaki.juahaki.core.quiz.model.UserAnswer;
import com.juahaki.juahaki.core.quiz.model.UserQuizAttempt;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;


@Repository
public interface UserAnswerRepository extends JpaRepository<UserAnswer, Long>, UserAnswerCustomRepository {
    boolean existsByAttemptAndQuestion(UserQuizAttempt attempt, CivicQuestion question);
}