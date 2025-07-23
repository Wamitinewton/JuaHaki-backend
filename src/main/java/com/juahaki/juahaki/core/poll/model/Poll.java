package com.juahaki.juahaki.core.poll.model;

import com.juahaki.juahaki.core.user.model.User;
import com.juahaki.juahaki.shared.enums.PollCategory;
import com.juahaki.juahaki.shared.enums.PollStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "polls", indexes = {
        @Index(name = "idx_poll_status", columnList = "status"),
        @Index(name = "idx_poll_category", columnList = "category"),
        @Index(name = "idx_poll_created_at", columnList = "createdAt"),
        @Index(name = "idx_poll_creator_id", columnList = "createdBy"),
        @Index(name = "idx_poll_end_date", columnList = "endDate"),
        @Index(name = "idx_poll_status_category", columnList = "status, category")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = false)
public class Poll {

    @Column(nullable = false)
    LocalDateTime endDate;
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false, length = 300)
    private String title;
    @Column(columnDefinition = "TEXT")
    private String description;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PollCategory category;
    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PollStatus status = PollStatus.DRAFT;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "createdBy", nullable = false,
            foreignKey = @ForeignKey(name = "fk_poll_creator"))
    private User creator;
    @Column(nullable = false)
    private LocalDateTime startDate;
    @Builder.Default
    @Column(nullable = false)
    private Boolean allowAnonymousVoting = true;

    @Builder.Default
    @Column(nullable = false)
    private Boolean allowOpinions = true;

    @Builder.Default
    @Column(nullable = false)
    private Long totalVotes = 0L;

    @Builder.Default
    @Column(nullable = false)
    private Long yesVotes = 0L;

    @Builder.Default
    @Column(nullable = false)
    private Long noVotes = 0L;

    @Builder.Default
    @Column(nullable = false)
    private Long neutralVotes = 0L;

    @Builder.Default
    @Column(nullable = false)
    private Long totalOpinions = 0L;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @CreationTimestamp
    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @OneToMany(mappedBy = "poll", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<PollAttachment> attachments;

    @OneToMany(mappedBy = "poll", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<PollVote> votes;

    @OneToMany(mappedBy = "poll", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<PollOpinion> opinions;

    public Boolean isActive() {
        return status == PollStatus.ACTIVE &&
                LocalDateTime.now().isBefore(endDate) &&
                LocalDateTime.now().isAfter(startDate);
    }

    public boolean isExpired() {
        return LocalDateTime.now().isAfter(endDate);
    }

    public double getYesPercentage() {
        return totalVotes > 0 ? (yesVotes * 100.0) / totalVotes : 0.0;
    }

    public double getNoPercentage() {
        return totalVotes > 0 ? (noVotes * 100.0) / totalVotes : 0.0;
    }

    public double getNeutralPercentage() {
        return totalVotes > 0 ? (neutralVotes * 100.0) / totalVotes : 0.0;
    }
}
