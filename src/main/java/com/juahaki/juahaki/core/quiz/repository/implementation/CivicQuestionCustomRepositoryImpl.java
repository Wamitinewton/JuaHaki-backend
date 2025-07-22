package com.juahaki.juahaki.core.quiz.repository.implementation;


import com.juahaki.juahaki.core.quiz.model.CivicQuestion;
import com.juahaki.juahaki.core.quiz.model.DailyQuiz;
import com.juahaki.juahaki.core.quiz.repository.CivicQuestionCustomRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public class CivicQuestionCustomRepositoryImpl implements CivicQuestionCustomRepository {

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    public Optional<CivicQuestion> findByQuizAndNumber(DailyQuiz dailyQuiz, int questionNumber) {
        String jpql = "SELECT cq FROM CivicQuestion cq WHERE cq.dailyQuiz = :dailyQuiz AND cq.questionNumber = :questionNumber";
        TypedQuery<CivicQuestion> query = entityManager.createQuery(jpql, CivicQuestion.class);
        query.setParameter("dailyQuiz", dailyQuiz);
        query.setParameter("questionNumber", questionNumber);
        return query.getResultList().stream().findFirst();
    }
}
