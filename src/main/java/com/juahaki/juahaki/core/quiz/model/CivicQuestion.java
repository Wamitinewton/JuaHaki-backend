package com.juahaki.juahaki.core.quiz.model;

import jakarta.persistence.*;
import lombok.*;

import java.util.List;

@Entity
@Table(name = "civic_questions")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = false)
public class CivicQuestion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "daily_quiz_id", nullable = false)
    private DailyQuiz dailyQuiz;

    @Column(nullable = false)
    private int questionNumber;

    @Column(nullable = false, length = 1000)
    private String questionText;

    @Column(length = 2000)
    private String explanation;

    @Column(nullable = false)
    private String category;

    @Column(nullable = false)
    private String difficulty;

    @OneToMany(mappedBy = "question", cascade = CascadeType.ALL, fetch = FetchType.EAGER)
    @OrderBy("optionLetter ASC")
    private List<QuestionOption> options;

    @Column(nullable = false)
    private String correctAnswer;

    @Column(length = 1000)
    private String sourceReference;

    public String getCorrectOptionText() {
        return options.stream()
                .filter(option -> option.getOptionLetter().equals(this.correctAnswer))
                .findFirst()
                .map(QuestionOption::getOptionText)
                .orElse("");
    }
}
