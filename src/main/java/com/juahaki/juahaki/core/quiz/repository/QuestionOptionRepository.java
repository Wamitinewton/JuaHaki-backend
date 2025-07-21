package com.juahaki.juahaki.core.quiz.repository;

import com.juahaki.juahaki.core.quiz.model.QuestionOption;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface QuestionOptionRepository extends JpaRepository<QuestionOption, Long> {

}