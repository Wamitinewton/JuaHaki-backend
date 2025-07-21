package com.juahaki.juahaki.core.quiz.model;

import com.juahaki.juahaki.shared.enums.QuizStatus;
import com.juahaki.juahaki.core.user.model.User;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "user_quiz_attempts")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = false)
public class UserQuizAttempt {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "daily_quiz_id", nullable = false)
    private DailyQuiz dailyQuiz;

    @Column(nullable = false, unique = true)
    private String sessionId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private QuizStatus status;

    @Column(nullable = false)
    private int totalQuestions;

    @Column(nullable = false)
    private int questionsAnswered = 0;

    @Column(nullable = false)
    private int correctAnswers = 0;

    @Column(nullable = false)
    private int score = 0; // Percentage

    @Column(nullable = false)
    private LocalDateTime startedAt;

    @Column
    private LocalDateTime completedAt;

    @Column
    private Long durationSeconds;

    @OneToMany(mappedBy = "attempt", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<UserAnswer> answers;

    @PrePersist
    protected void onCreate() {
        this.startedAt = LocalDateTime.now();
        this.status = QuizStatus.ACTIVE;
    }

    public void completeQuiz() {
        this.completedAt = LocalDateTime.now();
        this.status = QuizStatus.COMPLETED;
        this.durationSeconds = java.time.Duration.between(this.startedAt, this.completedAt).getSeconds();
        this.score = this.totalQuestions > 0 ? (this.correctAnswers * 100) / this.totalQuestions : 0;
    }

    public boolean isExpired() {
        // Quiz session expires after 600 minutes of inactivity
        return this.status == QuizStatus.ACTIVE &&
                LocalDateTime.now().isAfter(this.startedAt.plusMinutes(600));
    }

    public String getPerformanceLevel() {
        if (score >= 80) return "Excellent";
        if (score >= 70) return "Good";
        if (score >= 60) return "Fair";
        return "Needs Improvement";
    }
}
