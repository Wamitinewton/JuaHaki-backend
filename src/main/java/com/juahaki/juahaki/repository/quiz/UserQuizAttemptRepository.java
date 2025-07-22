package com.juahaki.juahaki.repository.quiz;

import com.juahaki.juahaki.enums.QuizStatus;
import com.juahaki.juahaki.model.quiz.DailyQuiz;
import com.juahaki.juahaki.model.quiz.UserQuizAttempt;
import com.juahaki.juahaki.model.user.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;


@Repository
public interface UserQuizAttemptRepository extends JpaRepository<UserQuizAttempt, Long>, UserQuizAttemptCustomRepository {
    Optional<UserQuizAttempt> findBySessionId(String sessionId);
    List<UserQuizAttempt> findByStatus(QuizStatus status);
    boolean existsByUserAndDailyQuiz(User user, DailyQuiz dailyQuiz);
}