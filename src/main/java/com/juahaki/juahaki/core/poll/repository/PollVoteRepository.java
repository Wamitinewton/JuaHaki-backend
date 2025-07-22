package com.juahaki.juahaki.core.poll.repository;

import com.juahaki.juahaki.core.poll.model.Poll;
import com.juahaki.juahaki.core.poll.model.PollVote;
import com.juahaki.juahaki.core.user.model.User;
import com.juahaki.juahaki.shared.enums.VoteChoice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface PollVoteRepository extends JpaRepository<PollVote, Long> {

    // Check if user has voted
    boolean existsByPollAndUser(Poll poll, User user);

    boolean existsByPollAndVoterFingerprint(Poll poll, String voterFingerprint);

    // Get user's vote on a poll
    Optional<PollVote> findByPollAndUser(Poll poll, User user);

    // Get votes by poll
    List<PollVote> findByPoll(Poll poll);

    // Count votes by choice
    long countByPollAndVoteChoice(Poll poll, VoteChoice voteChoice);

    // Count total votes for a poll
    long countByPoll(Poll poll);

    // Count anonymous votes
    long countByPollAndIsAnonymousTrue(Poll poll);

    // Count registered user votes
    long countByPollAndIsAnonymousFalse(Poll poll);

    // Get votes by user
    List<PollVote> findByUserOrderByCreatedAtDesc(User user);

    // Get recent votes for a poll
    @Query("SELECT pv FROM PollVote pv WHERE pv.poll = :poll ORDER BY pv.createdAt DESC")
    List<PollVote> findRecentVotesByPoll(@Param("poll") Poll poll);

    // Vote statistics by date range
    @Query("SELECT pv.voteChoice, COUNT(pv) FROM PollVote pv WHERE pv.poll = :poll AND pv.createdAt BETWEEN :startDate AND :endDate GROUP BY pv.voteChoice")
    List<Object[]> findVoteStatsByPollAndDateRange(@Param("poll") Poll poll,
                                                   @Param("startDate") LocalDateTime startDate,
                                                   @Param("endDate") LocalDateTime endDate);

    // User voting activity
    @Query("SELECT COUNT(pv) FROM PollVote pv WHERE pv.user = :user AND pv.createdAt >= :date")
    long countUserVotesSince(@Param("user") User user, @Param("date") LocalDateTime date);
}
