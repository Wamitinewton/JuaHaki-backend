package com.juahaki.juahaki.repository.quiz;

import com.juahaki.juahaki.model.quiz.DailyQuiz;
import com.juahaki.juahaki.model.quiz.UserQuizAttempt;
import com.juahaki.juahaki.model.user.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface UserQuizAttemptCustomRepository {
    Optional<UserQuizAttempt> findByUserAndQuizDate(User user, LocalDate date);
    Optional<UserQuizAttempt> findByUserAndDailyQuiz(User user, DailyQuiz dailyQuiz);
    List<UserQuizAttempt> findExpiredActiveSessions(LocalDateTime cutoffTime);
    List<UserQuizAttempt> findLeaderboardByQuiz(DailyQuiz dailyQuiz);
    Page<UserQuizAttempt> findLeaderboardByQuiz(DailyQuiz dailyQuiz, Pageable pageable);
    long countCompletedAttemptsByQuiz(DailyQuiz dailyQuiz);
    long countTotalAttemptsByQuiz(DailyQuiz dailyQuiz);
    Double findAverageScoreByQuiz(DailyQuiz dailyQuiz);
    List<UserQuizAttempt> findCompletedAttemptsByUser(User user);
}
