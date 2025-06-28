package com.juahaki.juahaki.repository.quiz;

import com.juahaki.juahaki.model.quiz.CivicQuestion;
import com.juahaki.juahaki.model.quiz.DailyQuiz;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CivicQuestionRepository extends JpaRepository<CivicQuestion, Long> {

    List<CivicQuestion> findByDailyQuizOrderByQuestionNumber(DailyQuiz dailyQuiz);

    Optional<CivicQuestion> findByDailyQuizAndQuestionNumber(DailyQuiz dailyQuiz, int questionNumber);

    @Query("SELECT cq FROM CivicQuestion cq WHERE cq.dailyQuiz.id = :quizId ORDER BY cq.questionNumber")
    List<CivicQuestion> findByDailyQuizIdOrderByQuestionNumber(@Param("quizId") Long quizId);

    @Query("SELECT cq FROM CivicQuestion cq WHERE cq.dailyQuiz = :dailyQuiz AND cq.questionNumber = :questionNumber")
    Optional<CivicQuestion> findByQuizAndNumber(@Param("dailyQuiz") DailyQuiz dailyQuiz,
                                                @Param("questionNumber") int questionNumber);

    @Query("SELECT COUNT(cq) FROM CivicQuestion cq WHERE cq.dailyQuiz = :dailyQuiz")
    int countByDailyQuiz(@Param("dailyQuiz") DailyQuiz dailyQuiz);

    @Query("SELECT cq FROM CivicQuestion cq WHERE cq.category = :category")
    List<CivicQuestion> findByCategory(@Param("category") String category);

    @Query("SELECT cq FROM CivicQuestion cq WHERE cq.difficulty = :difficulty")
    List<CivicQuestion> findByDifficulty(@Param("difficulty") String difficulty);

    @Query("SELECT DISTINCT cq.category FROM CivicQuestion cq")
    List<String> findDistinctCategories();

    @Query("SELECT DISTINCT cq.difficulty FROM CivicQuestion cq")
    List<String> findDistinctDifficulties();
}
