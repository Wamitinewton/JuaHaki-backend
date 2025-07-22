package com.juahaki.juahaki.repository.quiz.implementation;

import com.juahaki.juahaki.model.quiz.UserAnswer;
import com.juahaki.juahaki.model.quiz.UserQuizAttempt;
import com.juahaki.juahaki.repository.quiz.UserAnswerCustomRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class UserAnswerCustomRepositoryImpl implements UserAnswerCustomRepository {

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    public List<UserAnswer> findByAttemptOrderByQuestionNumber(UserQuizAttempt attempt) {
        String jpql = "SELECT ua FROM UserAnswer ua WHERE ua.attempt = :attempt ORDER BY ua.question.questionNumber";
        TypedQuery<UserAnswer> query = entityManager.createQuery(jpql, UserAnswer.class);
        query.setParameter("attempt", attempt);
        return query.getResultList();
    }
}