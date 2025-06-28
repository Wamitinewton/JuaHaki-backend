package com.juahaki.juahaki.repository.quiz;

import com.juahaki.juahaki.model.quiz.CivicQuestion;
import com.juahaki.juahaki.model.quiz.QuestionOption;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface QuestionOptionRepository extends JpaRepository<QuestionOption, Long> {

    List<QuestionOption> findByQuestionOrderByOptionLetter(CivicQuestion question);

    Optional<QuestionOption> findByQuestionAndOptionLetter(CivicQuestion question, String optionLetter);

    @Query("SELECT qo FROM QuestionOption qo WHERE qo.question.id = :questionId ORDER BY qo.optionLetter")
    List<QuestionOption> findByQuestionIdOrderByOptionLetter(@Param("questionId") Long questionId);

    @Query("SELECT COUNT(qo) FROM QuestionOption qo WHERE qo.question = :question")
    int countByQuestion(@Param("question") CivicQuestion civicQuestion);

    void deleteByQuestion(CivicQuestion civicQuestion);
}