package com.juahaki.juahaki.core.poll.mapper;

import com.juahaki.juahaki.core.poll.dto.voting.SubmitVoteRequest;
import com.juahaki.juahaki.core.poll.dto.voting.SubmitVoteResponse;
import com.juahaki.juahaki.core.poll.dto.voting.VoteStatusResponse;
import com.juahaki.juahaki.core.poll.model.Poll;
import com.juahaki.juahaki.core.poll.model.PollVote;
import com.juahaki.juahaki.core.user.model.User;
import com.juahaki.juahaki.shared.enums.VoteChoice;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * Mapper for poll voting operations.
 * Handles conversion between voting DTOs and entities, and creates voting responses.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class PollVotingMapper {

    /**
     * Convert SubmitVoteRequest to PollVote entity.
     *
     * @param request          the vote request
     * @param poll             the poll being voted on
     * @param voter            the user voting (null for anonymous votes)
     * @param voterFingerprint unique identifier for anonymous voters
     * @param ipAddress        voter's IP address
     * @param userAgent        voter's user agent
     * @return new PollVote entity
     */
    public PollVote toEntity(SubmitVoteRequest request, Poll poll, User voter,
                             String voterFingerprint, String ipAddress, String userAgent) {
        validateVoteRequest(request, poll);

        return PollVote.builder()
                .poll(poll)
                .user(voter)
                .voteChoice(request.getVoteChoice())
                .isAnonymous(request.getIsAnonymous())
                .voterFingerprint(voterFingerprint)
                .ipAddress(ipAddress)
                .userAgent(userAgent)
                .createdAt(LocalDateTime.now())
                .build();
    }

    /**
     * Create successful vote submission response.
     *
     * @param vote the submitted vote
     * @param poll the updated poll with current vote counts
     * @return vote submission response
     */
    public SubmitVoteResponse toSuccessResponse(PollVote vote, Poll poll) {
        if (vote == null || poll == null) {
            throw new IllegalArgumentException("Vote and poll cannot be null");
        }

        SubmitVoteResponse.VoteResultsSummary resultsSummary = buildVoteResultsSummary(poll);

        return SubmitVoteResponse.builder()
                .success(true)
                .message("Vote submitted successfully")
                .voteChoice(vote.getVoteChoice())
                .isAnonymous(vote.getIsAnonymous())
                .votedAt(vote.getCreatedAt())
                .currentResults(resultsSummary)
                .build();
    }

    /**
     * Create error vote submission response.
     *
     * @param errorMessage the error message
     * @return error response
     */
    public SubmitVoteResponse toErrorResponse(String errorMessage) {
        return SubmitVoteResponse.builder()
                .success(false)
                .message(errorMessage)
                .build();
    }

    /**
     * Convert PollVote to VoteStatusResponse.
     *
     * @param vote the user's vote (null if user hasn't voted)
     * @return vote status response
     */
    public VoteStatusResponse toVoteStatusResponse(PollVote vote) {
        if (vote == null) {
            return VoteStatusResponse.builder()
                    .hasVoted(false)
                    .build();
        }

        return VoteStatusResponse.builder()
                .hasVoted(true)
                .userVote(vote.getVoteChoice())
                .isAnonymous(vote.getIsAnonymous())
                .votedAt(vote.getCreatedAt())
                .build();
    }

    /**
     * Update existing vote entity with new vote choice.
     *
     * @param existingVote  the existing vote to update
     * @param newVoteChoice the new vote choice
     */
    public void updateVoteChoice(PollVote existingVote, VoteChoice newVoteChoice) {
        if (existingVote == null) {
            throw new IllegalArgumentException("Existing vote cannot be null");
        }

        if (newVoteChoice == null) {
            throw new IllegalArgumentException("New vote choice cannot be null");
        }

        existingVote.setVoteChoice(newVoteChoice);
    }

    /**
     * Build vote results summary from poll data.
     *
     * @param poll the poll with current vote counts
     * @return vote results summary
     */
    private SubmitVoteResponse.VoteResultsSummary buildVoteResultsSummary(Poll poll) {
        return SubmitVoteResponse.VoteResultsSummary.builder()
                .totalVotes(poll.getTotalVotes())
                .yesVotes(poll.getYesVotes())
                .noVotes(poll.getNoVotes())
                .neutralVotes(poll.getNeutralVotes())
                .yesPercentage(poll.getYesPercentage())
                .noPercentage(poll.getNoPercentage())
                .neutralPercentage(poll.getNeutralPercentage())
                .build();
    }

    /**
     * Validate vote submission request.
     *
     * @param request the vote request to validate
     * @param poll    the poll being voted on
     */
    private void validateVoteRequest(SubmitVoteRequest request, Poll poll) {
        if (request == null) {
            throw new IllegalArgumentException("Vote request cannot be null");
        }

        if (poll == null) {
            throw new IllegalArgumentException("Poll cannot be null");
        }

        if (request.getPollId() == null) {
            throw new IllegalArgumentException("Poll ID is required");
        }

        if (!request.getPollId().equals(poll.getId())) {
            throw new IllegalArgumentException("Poll ID mismatch");
        }

        if (request.getVoteChoice() == null) {
            throw new IllegalArgumentException("Vote choice is required");
        }

        if (request.getIsAnonymous() == null) {
            throw new IllegalArgumentException("Anonymous voting preference must be specified");
        }
    }

    /**
     * Create a response for successful vote change operation.
     *
     * @param originalVote the original vote
     * @param updatedVote  the updated vote
     * @param poll         the poll with updated counts
     * @return vote change response
     */
    public SubmitVoteResponse toVoteChangeResponse(PollVote originalVote, PollVote updatedVote, Poll poll) {
        if (originalVote == null || updatedVote == null || poll == null) {
            throw new IllegalArgumentException("Original vote, updated vote, and poll cannot be null");
        }

        SubmitVoteResponse.VoteResultsSummary resultsSummary = buildVoteResultsSummary(poll);

        String message = String.format("Vote changed from %s to %s successfully",
                originalVote.getVoteChoice().name(),
                updatedVote.getVoteChoice().name());

        return SubmitVoteResponse.builder()
                .success(true)
                .message(message)
                .voteChoice(updatedVote.getVoteChoice())
                .isAnonymous(updatedVote.getIsAnonymous())
                .votedAt(updatedVote.getCreatedAt())
                .currentResults(resultsSummary)
                .build();
    }

    /**
     * Create a response for successful vote withdrawal.
     *
     * @param withdrawnVote the vote that was withdrawn
     * @param poll          the poll with updated counts
     * @return vote withdrawal response
     */
    public SubmitVoteResponse toVoteWithdrawalResponse(PollVote withdrawnVote, Poll poll) {
        if (withdrawnVote == null || poll == null) {
            throw new IllegalArgumentException("Withdrawn vote and poll cannot be null");
        }

        SubmitVoteResponse.VoteResultsSummary resultsSummary = buildVoteResultsSummary(poll);

        return SubmitVoteResponse.builder()
                .success(true)
                .message("Vote withdrawn successfully")
                .voteChoice(null)
                .isAnonymous(withdrawnVote.getIsAnonymous())
                .votedAt(null)
                .currentResults(resultsSummary)
                .build();
    }
}
