package com.juahaki.juahaki.core.quiz.repository.implementation;


import com.juahaki.juahaki.core.quiz.model.DailyQuiz;
import com.juahaki.juahaki.core.quiz.model.UserQuizAttempt;
import com.juahaki.juahaki.core.quiz.repository.UserQuizAttemptCustomRepository;
import com.juahaki.juahaki.core.user.model.User;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public class UserQuizAttemptCustomRepositoryImpl implements UserQuizAttemptCustomRepository {

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    public Optional<UserQuizAttempt> findByUserAndQuizDate(User user, LocalDate date) {
        String jpql = "SELECT uqa FROM UserQuizAttempt uqa WHERE uqa.user = :user AND uqa.dailyQuiz.quizDate = :date";
        TypedQuery<UserQuizAttempt> query = entityManager.createQuery(jpql, UserQuizAttempt.class);
        query.setParameter("user", user);
        query.setParameter("date", date);

        return query.getResultList().stream().findFirst();
    }

    @Override
    public Optional<UserQuizAttempt> findByUserAndDailyQuiz(User user, DailyQuiz dailyQuiz) {
        String jpql = "SELECT uqa FROM UserQuizAttempt uqa WHERE uqa.user = :user AND uqa.dailyQuiz = :dailyQuiz";
        TypedQuery<UserQuizAttempt> query = entityManager.createQuery(jpql, UserQuizAttempt.class);
        query.setParameter("user", user);
        query.setParameter("dailyQuiz", dailyQuiz);

        return query.getResultList().stream().findFirst();
    }

    @Override
    public List<UserQuizAttempt> findExpiredActiveSessions(LocalDateTime cutoffTime) {
        String jpql = "SELECT uqa FROM UserQuizAttempt uqa WHERE uqa.status = 'ACTIVE' AND uqa.startedAt < :cutoffTime";
        TypedQuery<UserQuizAttempt> query = entityManager.createQuery(jpql, UserQuizAttempt.class);
        query.setParameter("cutoffTime", cutoffTime);

        return query.getResultList();
    }

    @Override
    public List<UserQuizAttempt> findLeaderboardByQuiz(DailyQuiz dailyQuiz) {
        String jpql = "SELECT uqa FROM UserQuizAttempt uqa WHERE uqa.dailyQuiz = :dailyQuiz AND uqa.status = 'COMPLETED' " +
                "ORDER BY uqa.score DESC, uqa.durationSeconds ASC";
        TypedQuery<UserQuizAttempt> query = entityManager.createQuery(jpql, UserQuizAttempt.class);
        query.setParameter("dailyQuiz", dailyQuiz);

        return query.getResultList();
    }

    @Override
    public Page<UserQuizAttempt> findLeaderboardByQuiz(DailyQuiz dailyQuiz, Pageable pageable) {
        String jpql = "SELECT uqa FROM UserQuizAttempt uqa WHERE uqa.dailyQuiz = :dailyQuiz AND uqa.status = 'COMPLETED' " +
                "ORDER BY uqa.score DESC, uqa.durationSeconds ASC";
        TypedQuery<UserQuizAttempt> query = entityManager.createQuery(jpql, UserQuizAttempt.class);
        query.setParameter("dailyQuiz", dailyQuiz);
        query.setFirstResult((int) pageable.getOffset());
        query.setMaxResults(pageable.getPageSize());

        List<UserQuizAttempt> results = query.getResultList();
        String countJpql = "SELECT COUNT(uqa) FROM UserQuizAttempt uqa WHERE uqa.dailyQuiz = :dailyQuiz AND uqa.status = 'COMPLETED'";
        TypedQuery<Long> countQuery = entityManager.createQuery(countJpql, Long.class);
        countQuery.setParameter("dailyQuiz", dailyQuiz);
        Long total = countQuery.getSingleResult();

        return new PageImpl<>(results, pageable, total);
    }

    @Override
    public long countCompletedAttemptsByQuiz(DailyQuiz dailyQuiz) {
        String jpql = "SELECT COUNT(uqa) FROM UserQuizAttempt uqa WHERE uqa.dailyQuiz = :dailyQuiz AND uqa.status = 'COMPLETED'";
        TypedQuery<Long> query = entityManager.createQuery(jpql, Long.class);
        query.setParameter("dailyQuiz", dailyQuiz);

        return query.getSingleResult();
    }

    @Override
    public long countTotalAttemptsByQuiz(DailyQuiz dailyQuiz) {
        String jpql = "SELECT COUNT(uqa) FROM UserQuizAttempt uqa WHERE uqa.dailyQuiz = :dailyQuiz";
        TypedQuery<Long> query = entityManager.createQuery(jpql, Long.class);
        query.setParameter("dailyQuiz", dailyQuiz);

        return query.getSingleResult();
    }

    @Override
    public Double findAverageScoreByQuiz(DailyQuiz dailyQuiz) {
        String jpql = "SELECT AVG(uqa.score) FROM UserQuizAttempt uqa WHERE uqa.dailyQuiz = :dailyQuiz AND uqa.status = 'COMPLETED'";
        TypedQuery<Double> query = entityManager.createQuery(jpql, Double.class);
        query.setParameter("dailyQuiz", dailyQuiz);

        Double result = query.getSingleResult();
        return result != null ? result : 0.0;
    }

    @Override
    public List<UserQuizAttempt> findCompletedAttemptsByUser(User user) {
        String jpql = "SELECT uqa FROM UserQuizAttempt uqa WHERE uqa.user = :user AND uqa.status = 'COMPLETED' " +
                "ORDER BY uqa.startedAt DESC";
        TypedQuery<UserQuizAttempt> query = entityManager.createQuery(jpql, UserQuizAttempt.class);
        query.setParameter("user", user);

        return query.getResultList();
    }
}