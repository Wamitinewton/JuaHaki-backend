package com.juahaki.juahaki.core.quiz.repository;

import com.juahaki.juahaki.core.quiz.model.CivicQuestion;
import com.juahaki.juahaki.core.quiz.model.DailyQuiz;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CivicQuestionRepository extends JpaRepository<CivicQuestion, Long>, CivicQuestionCustomRepository {
    List<CivicQuestion> findByDailyQuizOrderByQuestionNumber(DailyQuiz dailyQuiz);
}
