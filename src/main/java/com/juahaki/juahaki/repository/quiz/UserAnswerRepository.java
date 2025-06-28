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

    List<UserAnswer> findByAttemptOrderByQuestionQuestionNumber(UserQuizAttempt attempt);

    Optional<UserAnswer> findByAttemptAndQuestion(UserQuizAttempt attempt, CivicQuestion question);

    @Query("SELECT ua FROM UserAnswer ua WHERE ua.attempt = :attempt ORDER BY ua.question.questionNumber")
    List<UserAnswer> findByAttemptOrderByQuestionNumber(@Param("attempt") UserQuizAttempt attempt);

    @Query("SELECT COUNT(ua) FROM UserAnswer ua WHERE ua.attempt = :attempt")
    int countByAttempt(@Param("attempt") UserQuizAttempt attempt);

    @Query("SELECT COUNT(ua) FROM UserAnswer ua WHERE ua.attempt = :attempt AND ua.isCorrect = true")
    int countCorrectAnswersByAttempt(@Param("attempt") UserQuizAttempt attempt);

    @Query("SELECT ua FROM UserAnswer ua WHERE ua.question = :question")
    List<UserAnswer> findByQuestion(@Param("question") CivicQuestion question);

    @Query("SELECT COUNT(ua) FROM UserAnswer ua WHERE ua.question = :question AND ua.isCorrect = true")
    long countCorrectAnswersByQuestion(@Param("question") CivicQuestion question);

    @Query("SELECT COUNT(ua) FROM UserAnswer ua WHERE ua.question = :question")
    long countTotalAnswersByQuestion(@Param("question") CivicQuestion question);

    @Query("SELECT ua.selectedAnswer, COUNT(ua) FROM UserAnswer ua WHERE ua.question = :question GROUP BY ua.selectedAnswer")
    List<Object[]> findAnswerDistributionByQuestion(@Param("question") CivicQuestion question);

    @Query("SELECT AVG(ua.timeSpentSeconds) FROM UserAnswer ua WHERE ua.question = :question AND ua.timeSpentSeconds IS NOT NULL")
    Double findAverageTimeSpentByQuestion(@Param("question") CivicQuestion question);

    @Query("SELECT ua FROM UserAnswer ua WHERE ua.question.category = :category AND ua.attempt.user.id = :userId")
    List<UserAnswer> findByUserAndCategory(@Param("userId") Long userId, @Param("category") String category);

    @Query("SELECT ua.question.category, COUNT(ua), SUM(CASE WHEN ua.isCorrect = true THEN 1 ELSE 0 END) FROM UserAnswer ua WHERE ua.attempt = :attempt GROUP BY ua.question.category")
    List<Object[]> findCategoryPerformanceByAttempt(@Param("attempt") UserQuizAttempt attempt);

    boolean existsByAttemptAndQuestion(UserQuizAttempt attempt, CivicQuestion question);
}