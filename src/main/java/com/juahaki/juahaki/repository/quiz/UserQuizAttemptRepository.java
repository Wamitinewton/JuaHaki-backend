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
public interface UserQuizAttemptRepository extends JpaRepository<UserQuizAttempt, Long> {

    Optional<UserQuizAttempt> findBySessionId(String sessionId);

    @Query("SELECT uqa FROM UserQuizAttempt uqa WHERE uqa.user = :user AND uqa.dailyQuiz.quizDate = :date")
    Optional<UserQuizAttempt> findByUserAndQuizDate(@Param("user") User user, @Param("date") LocalDate date);

    @Query("SELECT uqa FROM UserQuizAttempt uqa WHERE uqa.user = :user AND uqa.dailyQuiz = :dailyQuiz")
    Optional<UserQuizAttempt> findByUserAndDailyQuiz(@Param("user") User user, @Param("dailyQuiz") DailyQuiz dailyQuiz);

    @Query("SELECT uqa FROM UserQuizAttempt uqa WHERE uqa.status = :status")
    List<UserQuizAttempt> findByStatus(@Param("status") QuizStatus status);

    @Query("SELECT uqa FROM UserQuizAttempt uqa WHERE uqa.status = 'ACTIVE' AND uqa.startedAt < :cutoffTime")
    List<UserQuizAttempt> findExpiredActiveSessions(@Param("cutoffTime") LocalDateTime cutoffTime);

    @Query("SELECT uqa FROM UserQuizAttempt uqa WHERE uqa.dailyQuiz = :dailyQuiz AND uqa.status = 'COMPLETED' ORDER BY uqa.score DESC, uqa.durationSeconds ASC")
    List<UserQuizAttempt> findLeaderboardByQuiz(@Param("dailyQuiz") DailyQuiz dailyQuiz);

    @Query("SELECT uqa FROM UserQuizAttempt uqa WHERE uqa.dailyQuiz = :dailyQuiz AND uqa.status = 'COMPLETED' ORDER BY uqa.score DESC, uqa.durationSeconds ASC")
    Page<UserQuizAttempt> findLeaderboardByQuiz(@Param("dailyQuiz") DailyQuiz dailyQuiz, Pageable pageable);

    @Query("SELECT COUNT(uqa) FROM UserQuizAttempt uqa WHERE uqa.dailyQuiz = :dailyQuiz AND uqa.status = 'COMPLETED'")
    long countCompletedAttemptsByQuiz(@Param("dailyQuiz") DailyQuiz dailyQuiz);

    @Query("SELECT COUNT(uqa) FROM UserQuizAttempt uqa WHERE uqa.dailyQuiz = :dailyQuiz")
    long countTotalAttemptsByQuiz(@Param("dailyQuiz") DailyQuiz dailyQuiz);

    @Query("SELECT AVG(uqa.score) FROM UserQuizAttempt uqa WHERE uqa.dailyQuiz = :dailyQuiz AND uqa.status = 'COMPLETED'")
    Double findAverageScoreByQuiz(@Param("dailyQuiz") DailyQuiz dailyQuiz);

    @Query("SELECT uqa FROM UserQuizAttempt uqa WHERE uqa.user = :user AND uqa.status = 'COMPLETED' ORDER BY uqa.startedAt DESC")
    List<UserQuizAttempt> findCompletedAttemptsByUser(@Param("user") User user);

    boolean existsByUserAndDailyQuiz(User user, DailyQuiz dailyQuiz);
}