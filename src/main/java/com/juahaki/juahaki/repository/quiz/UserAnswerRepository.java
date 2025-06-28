package com.juahaki.juahaki.repository.quiz;

import com.juahaki.juahaki.model.quiz.CivicQuestion;
import com.juahaki.juahaki.model.quiz.UserAnswer;
import com.juahaki.juahaki.model.quiz.UserQuizAttempt;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserAnswerRepository extends JpaRepository<UserAnswer, Long> {

    @Query("SELECT ua FROM UserAnswer ua WHERE ua.attempt = :attempt ORDER BY ua.question.questionNumber")
    List<UserAnswer> findByAttemptOrderByQuestionNumber(@Param("attempt") UserQuizAttempt attempt);

    boolean existsByAttemptAndQuestion(UserQuizAttempt attempt, CivicQuestion question);
}