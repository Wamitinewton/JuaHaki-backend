package com.juahaki.juahaki.core.poll.repository;

import com.juahaki.juahaki.core.poll.model.PollOpinion;
import com.juahaki.juahaki.core.poll.model.PollOpinionReaction;
import com.juahaki.juahaki.core.user.model.User;
import com.juahaki.juahaki.shared.enums.ReactionType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PollOpinionReactionRepository extends JpaRepository<PollOpinionReaction, Long> {

    // Check if user has reacted
    boolean existsByOpinionAndReactor(PollOpinion opinion, User reactor);

    // Check anonymous reaction by fingerprint
    boolean existsByOpinionAndReactorFingerprint(PollOpinion opinion, String reactorFingerprint);

    // Get user's reaction
    Optional<PollOpinionReaction> findByOpinionAndReactor(PollOpinion opinion, User reactor);

    // Get reactions by type
    List<PollOpinionReaction> findByOpinionAndReactionType(PollOpinion opinion, ReactionType reactionType);

    // Count reactions
    long countByOpinionAndReactionType(PollOpinion opinion, ReactionType reactionType);

    long countByOpinion(PollOpinion opinion);

    // User reactions
    List<PollOpinionReaction> findByReactorOrderByCreatedAtDesc(User reactor);

    // Anonymous reactions count
    long countByOpinionAndIsAnonymousTrue(PollOpinion opinion);

    // Registered user reactions count
    long countByOpinionAndIsAnonymousFalse(PollOpinion opinion);

    // Reaction statistics
    @Query("SELECT por.reactionType, COUNT(por) FROM PollOpinionReaction por WHERE por.opinion = :opinion GROUP BY por.reactionType")
    List<Object[]> findReactionStatsByOpinion(@Param("opinion") PollOpinion opinion);
}
