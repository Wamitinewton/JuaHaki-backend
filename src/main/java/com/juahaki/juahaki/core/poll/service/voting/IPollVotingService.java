package com.juahaki.juahaki.core.poll.service.voting;

import com.juahaki.juahaki.core.poll.dto.voting.SubmitVoteRequest;
import com.juahaki.juahaki.core.poll.dto.voting.SubmitVoteResponse;
import com.juahaki.juahaki.core.poll.dto.voting.VoteStatusResponse;
import com.juahaki.juahaki.core.poll.model.Poll;
import com.juahaki.juahaki.shared.exception.CustomException;
import jakarta.servlet.http.HttpServletRequest;

/**
 * Service interface for managing poll voting operations.
 * Handles vote submission, validation, and vote status tracking.
 * Supports both anonymous and authenticated voting with fraud prevention.
 */
public interface IPollVotingService {

    /**
     * Submits a vote (Yes/No/Neutral) for a specific poll.
     * Supports both anonymous and authenticated voting based on user preference.
     * Implements fraud prevention mechanisms including fingerprinting and IP tracking.
     *
     * @param submitVoteRequest the vote submission request containing poll ID, vote choice, and anonymity preference
     * @param request           the HTTP request to extract user details, IP address, and user agent
     * @return SubmitVoteResponse containing vote confirmation and updated poll statistics
     * @throws CustomException          if the poll is not found, not active, or voting is not allowed
     * @throws IllegalStateException    if the user has already voted on this poll
     * @throws IllegalArgumentException if the vote request is invalid
     */
    SubmitVoteResponse submitVote(SubmitVoteRequest submitVoteRequest, HttpServletRequest request);

    /**
     * Changes an existing vote to a new choice.
     * Only allowed if the poll configuration permits vote changes and within allowed timeframe.
     *
     * @param submitVoteRequest the new vote request containing updated vote choice
     * @param request           the HTTP request to extract user authentication details
     * @return SubmitVoteResponse containing confirmation of vote change and updated statistics
     * @throws CustomException       if vote changing is not allowed or user hasn't voted previously
     * @throws IllegalStateException if the change window has expired
     */
    SubmitVoteResponse changeVote(SubmitVoteRequest submitVoteRequest, HttpServletRequest request);

    /**
     * Retrieves the voting status for a user on a specific poll.
     * Shows whether they have voted, their vote choice, and voting timestamp.
     *
     * @param pollId  the unique identifier of the poll
     * @param request the HTTP request to extract user authentication details
     * @return VoteStatusResponse containing user's voting status and details
     */
    VoteStatusResponse getUserVoteStatus(Long pollId, HttpServletRequest request);

    /**
     * Removes a user's vote from a poll if withdrawal is permitted.
     * This operation is typically restricted and may only be available for a limited time.
     *
     * @param pollId  the unique identifier of the poll
     * @param request the HTTP request to extract user authentication details
     * @throws CustomException       if vote withdrawal is not allowed or user hasn't voted
     * @throws IllegalStateException if the withdrawal window has expired
     */
    void withdrawVote(Long pollId, HttpServletRequest request);

    /**
     * Validates a vote submission request against business rules and poll constraints.
     * Checks poll status, voting eligibility, and request validity.
     *
     * @param poll              the poll entity to vote on
     * @param submitVoteRequest the vote request to validate
     * @param request           the HTTP request for additional validation context
     * @throws IllegalArgumentException if the vote request is invalid
     * @throws CustomException          if voting is not allowed for this poll/user combination
     */
    void validateVoteSubmission(Poll poll, SubmitVoteRequest submitVoteRequest, HttpServletRequest request);

    /**
     * Checks if a user has already voted on a specific poll.
     * Uses multiple mechanisms including user ID, fingerprint, and IP tracking for anonymous votes.
     *
     * @param poll    the poll entity to check
     * @param request the HTTP request to extract user identification details
     * @return true if the user has already voted, false otherwise
     */
    boolean hasUserVoted(Poll poll, HttpServletRequest request);

    /**
     * Verifies if voting is currently allowed for a poll.
     * Considers poll status, timing constraints, and system-wide voting policies.
     *
     * @param poll the poll entity to check
     * @return true if voting is allowed, false otherwise
     */
    boolean isVotingAllowed(Poll poll);

    /**
     * Generates a unique fingerprint for anonymous voters to prevent duplicate voting.
     * Combines multiple client characteristics while respecting privacy.
     *
     * @param request the HTTP request to extract fingerprinting data
     * @return a unique fingerprint string for the voter
     */
    String generateVoterFingerprint(HttpServletRequest request);

    /**
     * Updates poll vote counts and statistics after a vote is cast.
     * This method ensures atomicity and consistency of vote tallies.
     *
     * @param poll               the poll entity to update
     * @param previousVoteChoice the previous vote choice if changing vote (null for new votes)
     * @param newVoteChoice      the new vote choice being cast
     */
    void updatePollVoteStatistics(Poll poll, String previousVoteChoice, String newVoteChoice);

    /**
     * Validates that a poll accepts the specified voting mode (anonymous/authenticated).
     * Some polls may restrict voting to authenticated users only.
     *
     * @param poll        the poll entity to check
     * @param isAnonymous whether the vote is being cast anonymously
     * @throws CustomException if the voting mode is not supported by the poll
     */
    void validateVotingMode(Poll poll, boolean isAnonymous);
}
