package com.juahaki.juahaki.core.poll.model;

import com.juahaki.juahaki.core.user.model.User;
import com.juahaki.juahaki.shared.enums.ReactionType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "poll_opinion_reactions",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_poll_opinion_reaction_user",
                        columnNames = {"opinionId", "userId"}),
                @UniqueConstraint(name = "uk_poll_opinion_reaction_fingerprint",
                        columnNames = {"opinionId", "reactorFingerprint"})
        },
        indexes = {
                @Index(name = "idx_poll_opinion_reaction_opinion_id", columnList = "opinionId"),
                @Index(name = "idx_poll_opinion_reaction_user_id", columnList = "userId"),
                @Index(name = "idx_poll_opinion_reaction_type", columnList = "reactionType"),
                @Index(name = "idx_poll_opinion_reaction_anonymous", columnList = "isAnonymous"),
                @Index(name = "idx_poll_opinion_reaction_fingerprint", columnList = "reactorFingerprint")
        })
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = false)
public class PollOpinionReaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "opinionId", nullable = false,
            foreignKey = @ForeignKey(name = "fk_poll_opinion_reaction_opinion"))
    private PollOpinion opinion;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "userId", nullable = true,
            foreignKey = @ForeignKey(name = "fk_poll_opinion_reaction_user"))
    private User reactor;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ReactionType reactionType;

    @Builder.Default
    @Column(nullable = false)
    private Boolean isAnonymous = false;

    @Column(length = 255)
    private String reactorFingerprint;

    @Column(length = 45)
    private String ipAddress;

    @Column(length = 500)
    private String userAgent;

    @CreationTimestamp
    @Column(nullable = false)
    private LocalDateTime createdAt;

    public boolean isFromRegisteredUser() {
        return reactor != null && !isAnonymous;
    }
}
