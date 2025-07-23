package com.juahaki.juahaki.core.poll.repository;

import com.juahaki.juahaki.core.poll.model.Poll;
import com.juahaki.juahaki.core.poll.model.PollOpinion;
import com.juahaki.juahaki.core.user.model.User;
import com.juahaki.juahaki.shared.enums.VoteChoice;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface PollOpinionRepository extends JpaRepository<PollOpinion, Long>, JpaSpecificationExecutor<PollOpinion> {
    List<PollOpinion> findByPoll(Poll poll);

    Page<PollOpinion> findByPollOrderByCreatedAtDesc(Poll poll, Pageable pageable);

    // Opinion by stance
    List<PollOpinion> findByPollAndStance(Poll poll, VoteChoice stance);

    long countByPollAndStance(Poll poll, VoteChoice stance);

    // User opinions
    Page<PollOpinion> findByAuthorOrderByCreatedAtDesc(User author, Pageable pageable);
    boolean existsByPollAndAuthor(Poll poll, User author);

    // Anonymous vs registered opinions
    List<PollOpinion> findByPollAndIsAnonymousTrue(Poll poll);

    List<PollOpinion> findByPollAndIsAnonymousFalse(Poll poll);

    long countByPollAndIsAnonymousTrue(Poll poll);

    long countByPollAndIsAnonymousFalse(Poll poll);

    // Top opinions by likes
    @Query("SELECT po FROM PollOpinion po WHERE po.poll = :poll ORDER BY po.likesCount DESC")
    Page<PollOpinion> findTopOpinionsByLikes(@Param("poll") Poll poll, Pageable pageable);

    // Recent opinions
    @Query("SELECT po FROM PollOpinion po WHERE po.poll = :poll ORDER BY po.createdAt DESC")
    Page<PollOpinion> findRecentOpinions(@Param("poll") Poll poll, Pageable pageable);

    // Search in opinions
    @Query("SELECT po FROM PollOpinion po WHERE po.poll = :poll AND LOWER(po.content) LIKE %:searchTerm%")
    Page<PollOpinion> findByPollAndContentContaining(@Param("poll") Poll poll,
                                                     @Param("searchTerm") String searchTerm,
                                                     Pageable pageable);

    // Opinions with minimum likes
    @Query("SELECT po FROM PollOpinion po WHERE po.poll = :poll AND po.likesCount >= :minLikes ORDER BY po.likesCount DESC")
    List<PollOpinion> findOpinionsWithMinimumLikes(@Param("poll") Poll poll, @Param("minLikes") Long minLikes);

    // Count opinions by poll
    long countByPoll(Poll poll);

    // Recent activity
    @Query("SELECT po FROM PollOpinion po WHERE po.createdAt >= :date ORDER BY po.createdAt DESC")
    List<PollOpinion> findRecentOpinionsSince(@Param("date") LocalDateTime date);

    // User's most liked opinions
    @Query("SELECT po FROM PollOpinion po WHERE po.author = :user ORDER BY po.likesCount DESC")
    List<PollOpinion> findUserTopOpinions(@Param("user") User user);
}
