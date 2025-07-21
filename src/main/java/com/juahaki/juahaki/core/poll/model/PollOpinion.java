package com.juahaki.juahaki.core.poll.model;

import com.juahaki.juahaki.core.user.model.User;
import com.juahaki.juahaki.shared.enums.VoteChoice;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "poll_opinions", indexes = {
        @Index(name = "idx_poll_opinion_poll_id", columnList = "pollId"),
        @Index(name = "idx_poll_opinion_user_id", columnList = "userId"),
        @Index(name = "idx_poll_opinion_stance", columnList = "stance"),
        @Index(name = "idx_poll_opinion_anonymous", columnList = "isAnonymous"),
        @Index(name = "idx_poll_opinion_created_at", columnList = "createdAt"),
        @Index(name = "idx_poll_opinion_likes", columnList = "likesCount"),
        @Index(name = "idx_poll_opinion_fingerprint", columnList = "authorFingerprint")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = false)
public class PollOpinion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pollId", nullable = false,
            foreignKey = @ForeignKey(name = "fk_poll_opinion_poll"))
    private Poll poll;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "userId", nullable = true,
            foreignKey = @ForeignKey(name = "fk_poll_opinion_user"))
    private User author;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String content;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private VoteChoice stance;

    @Builder.Default
    @Column(nullable = false)
    private Boolean isAnonymous = false;

    @Column(length = 255)
    private String authorFingerprint;

    @Column(length = 45)
    private String ipAddress;

    @Column(length = 500)
    private String userAgent;

    @Builder.Default
    @Column(nullable = false)
    private Long likesCount = 0L;

    @Builder.Default
    @Column(nullable = false)
    private Long dislikesCount = 0L;

    @CreationTimestamp
    @Column(nullable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @OneToMany(mappedBy = "opinion", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<PollOpinionAttachment> attachments;

    @OneToMany(mappedBy = "opinion", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<PollOpinionReaction> reactions;

    public boolean isFromRegisteredUser() {
        return author != null && !isAnonymous;
    }

    public double getLikePercentage() {
        long totalReactions = likesCount + dislikesCount;
        return totalReactions > 0 ? (likesCount * 100.0) / totalReactions : 0.0;
    }
}
