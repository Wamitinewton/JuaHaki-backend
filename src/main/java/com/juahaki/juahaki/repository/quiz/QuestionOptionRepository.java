package com.juahaki.juahaki.repository.quiz;

import com.juahaki.juahaki.model.quiz.QuestionOption;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface QuestionOptionRepository extends JpaRepository<QuestionOption, Long> {

}