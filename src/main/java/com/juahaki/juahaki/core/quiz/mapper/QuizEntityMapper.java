package com.juahaki.juahaki.core.quiz.mapper;

import com.juahaki.juahaki.core.quiz.dto.QuestionDto;
import com.juahaki.juahaki.core.quiz.model.CivicQuestion;
import com.juahaki.juahaki.core.quiz.model.DailyQuiz;
import com.juahaki.juahaki.core.quiz.model.QuestionOption;
import com.juahaki.juahaki.core.quiz.model.UserQuizAttempt;
import com.juahaki.juahaki.core.user.model.User;
import com.juahaki.juahaki.core.quiz.service.ai.CivicQuizAIService.QuestionData;
import com.juahaki.juahaki.core.quiz.service.ai.CivicQuizAIService.QuizGenerationResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
@Slf4j
public class QuizEntityMapper {

    public DailyQuiz createDailyQuiz(LocalDate quizDate, QuizGenerationResponse response) {
        LocalDateTime now = LocalDateTime.now();
        return DailyQuiz.builder()
                .quizDate(quizDate)
                .title(response.getTitle())
                .description(response.getDescription())
                .totalQuestions(response.getQuestions().size())
                .isActive(true)
                .expiresAt(now.plusHours(24))
                .build();
    }

    /**
     * Convert QuestionData list to CivicQuestion entities
     */
    public List<CivicQuestion> createQuestions(DailyQuiz quiz, List<QuestionData> questionData) {
        return questionData.stream().map(data -> {
            List<QuestionOption> options = data.getOptions().stream()
                    .map(optionData -> QuestionOption.builder()
                            .optionLetter(optionData.getLetter())
                            .optionText(optionData.getText())
                            .build())
                    .collect(Collectors.toList());

            CivicQuestion question = CivicQuestion.builder()
                    .dailyQuiz(quiz)
                    .questionNumber(data.getQuestionNumber())
                    .questionText(data.getQuestionText())
                    .explanation(data.getExplanation())
                    .category(data.getCategory())
                    .difficulty(data.getDifficulty())
                    .correctAnswer(data.getCorrectAnswer())
                    .sourceReference(data.getSourceReference())
                    .build();

            // Set bidirectional relationship
            options.forEach(option -> option.setQuestion(question));
            question.setOptions(options);

            return question;
        }).collect(Collectors.toList());
    }

    /**
     * Create UserQuizAttempt entity
     */
    public UserQuizAttempt createQuizAttempt(User user, DailyQuiz quiz, String sessionId) {
        return UserQuizAttempt.builder()
                .user(user)
                .dailyQuiz(quiz)
                .sessionId(sessionId)
                .totalQuestions(quiz.getTotalQuestions())
                .questionsAnswered(0)
                .correctAnswers(0)
                .score(0)
                .build();
    }

    /**
     * Convert CivicQuestion to DTO (optional utility method)
     */
    public QuestionDto convertToQuestionDto(CivicQuestion question) {
        List<QuestionDto.OptionDto> optionDtos = question.getOptions().stream()
                .map(option -> QuestionDto.OptionDto.builder()
                        .optionLetter(option.getOptionLetter())
                        .optionText(option.getOptionText())
                        .build())
                .collect(Collectors.toList());

        return QuestionDto.builder()
                .questionId(question.getId())
                .questionNumber(question.getQuestionNumber())
                .questionText(question.getQuestionText())
                .explanation(question.getExplanation())
                .category(question.getCategory())
                .difficulty(question.getDifficulty())
                .correctAnswer(question.getCorrectAnswer())
                .sourceReference(question.getSourceReference())
                .options(optionDtos)
                .build();
    }

    /**
     * Convert DTO back to CivicQuestion (optional utility method)
     */
    public CivicQuestion convertFromQuestionDto(QuestionDto dto) {
        List<QuestionOption> options = dto.getOptions().stream()
                .map(optionDto -> QuestionOption.builder()
                        .optionLetter(optionDto.getOptionLetter())
                        .optionText(optionDto.getOptionText())
                        .build())
                .collect(Collectors.toList());

        CivicQuestion question = CivicQuestion.builder()
                .id(dto.getQuestionId())
                .questionNumber(dto.getQuestionNumber())
                .questionText(dto.getQuestionText())
                .explanation(dto.getExplanation())
                .category(dto.getCategory())
                .difficulty(dto.getDifficulty())
                .correctAnswer(dto.getCorrectAnswer())
                .sourceReference(dto.getSourceReference())
                .options(options)
                .build();

        // Set bidirectional relationship
        options.forEach(option -> option.setQuestion(question));

        return question;
    }
}