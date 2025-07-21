package com.juahaki.juahaki.core.poll.model;

import com.juahaki.juahaki.core.user.model.User;
import com.juahaki.juahaki.shared.enums.VoteChoice;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "poll_votes",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_poll_vote_user", columnNames = {"pollId", "userId"}),
                @UniqueConstraint(name = "uk_poll_vote_fingerprint", columnNames = {"pollId", "voterFingerprint"})
        },
        indexes = {
                @Index(name = "idx_poll_vote_poll_id", columnList = "pollId"),
                @Index(name = "idx_poll_vote_user_id", columnList = "userId"),
                @Index(name = "idx_poll_vote_choice", columnList = "voteChoice"),
                @Index(name = "idx_poll_vote_anonymous", columnList = "isAnonymous"),
                @Index(name = "idx_poll_vote_fingerprint", columnList = "voterFingerprint"),
                @Index(name = "idx_poll_vote_created_at", columnList = "createdAt")
        })
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = false)
public class PollVote {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pollId", nullable = false,
            foreignKey = @ForeignKey(name = "fk_poll_vote_poll"))
    private Poll poll;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "userId", nullable = true,
            foreignKey = @ForeignKey(name = "fk_poll_vote_user"))
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private VoteChoice voteChoice;

    @Builder.Default
    @Column(nullable = false)
    private Boolean isAnonymous = false;

    @Column(length = 255)
    private String voterFingerprint;

    @Column(length = 45)
    private String ipAddress;

    @Column(length = 500)
    private String userAgent;

    @CreationTimestamp
    @Column(nullable = false)
    private LocalDateTime createdAt;

    public boolean isFromRegisteredUser() {
        return user != null && !isAnonymous;
    }
}
