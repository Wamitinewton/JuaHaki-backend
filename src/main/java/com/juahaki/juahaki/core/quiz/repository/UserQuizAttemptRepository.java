package com.juahaki.juahaki.core.quiz.repository;

import com.juahaki.juahaki.core.quiz.model.DailyQuiz;
import com.juahaki.juahaki.core.quiz.model.UserQuizAttempt;
import com.juahaki.juahaki.core.user.model.User;
import com.juahaki.juahaki.shared.enums.QuizStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;


@Repository
public interface UserQuizAttemptRepository extends JpaRepository<UserQuizAttempt, Long>, UserQuizAttemptCustomRepository {
    Optional<UserQuizAttempt> findBySessionId(String sessionId);

    List<UserQuizAttempt> findByStatus(QuizStatus status);

    boolean existsByUserAndDailyQuiz(User user, DailyQuiz dailyQuiz);
}