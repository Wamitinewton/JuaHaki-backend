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
public interface CivicQuestionRepository extends JpaRepository<CivicQuestion, Long>, CivicQuestionCustomRepository {
    List<CivicQuestion> findByDailyQuizOrderByQuestionNumber(DailyQuiz dailyQuiz);
}
