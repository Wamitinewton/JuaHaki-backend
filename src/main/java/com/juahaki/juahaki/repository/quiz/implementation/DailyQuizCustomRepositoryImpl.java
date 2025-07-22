package com.juahaki.juahaki.repository.quiz.implementation;


import com.juahaki.juahaki.model.quiz.DailyQuiz;
import com.juahaki.juahaki.repository.quiz.DailyQuizCustomRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.Optional;

@Repository
public class DailyQuizCustomRepositoryImpl implements DailyQuizCustomRepository {

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    public Optional<DailyQuiz> findActiveQuizByDate(LocalDate date) {
        String jpql = "SELECT dq FROM DailyQuiz dq WHERE dq.quizDate = :date AND dq.isActive = true";
        TypedQuery<DailyQuiz> query = entityManager.createQuery(jpql, DailyQuiz.class);
        query.setParameter("date", date);

        return query.getResultList().stream().findFirst();
    }

    @Override
    public Optional<DailyQuiz> findTodaysActiveQuiz() {
        String jpql = "SELECT dq FROM DailyQuiz dq WHERE dq.quizDate = CURRENT_DATE AND dq.isActive = true";
        TypedQuery<DailyQuiz> query = entityManager.createQuery(jpql, DailyQuiz.class);

        return query.getResultList().stream().findFirst();
    }
}