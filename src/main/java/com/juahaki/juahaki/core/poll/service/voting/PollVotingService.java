package com.juahaki.juahaki.core.poll.service.voting;

import com.juahaki.juahaki.core.poll.dto.voting.SubmitVoteRequest;
import com.juahaki.juahaki.core.poll.dto.voting.SubmitVoteResponse;
import com.juahaki.juahaki.core.poll.dto.voting.VoteStatusResponse;
import com.juahaki.juahaki.core.poll.mapper.PollVotingMapper;
import com.juahaki.juahaki.core.poll.model.Poll;
import com.juahaki.juahaki.core.poll.model.PollVote;
import com.juahaki.juahaki.core.poll.repository.PollRepository;
import com.juahaki.juahaki.core.poll.repository.PollVoteRepository;
import com.juahaki.juahaki.core.user.model.User;
import com.juahaki.juahaki.core.user.repository.UserRepository;
import com.juahaki.juahaki.shared.enums.VoteChoice;
import com.juahaki.juahaki.shared.exception.CustomException;
import com.juahaki.juahaki.shared.utils.jwt.JwtHelperService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class PollVotingService implements IPollVotingService {

    private final PollRepository pollRepository;
    private final PollVoteRepository pollVoteRepository;
    private final UserRepository userRepository;
    private final PollVotingMapper pollVotingMapper;
    private final JwtHelperService jwtHelperService;

    @Override
    @Transactional
    public SubmitVoteResponse submitVote(SubmitVoteRequest submitVoteRequest, HttpServletRequest request) {
        log.info("Processing vote submission for poll ID: {}", submitVoteRequest.getPollId());

        Poll poll = getPollById(submitVoteRequest.getPollId());
        validateVoteSubmission(poll, submitVoteRequest, request);

        User voter = getVoterIfAuthenticated(submitVoteRequest.getIsAnonymous(), request);

        validateNoDuplicateVote(poll, voter, request);

        PollVote vote = createVoteEntity(submitVoteRequest, poll, voter, request);
        PollVote savedVote = pollVoteRepository.save(vote);

        updatePollVoteStatistics(poll, null, submitVoteRequest.getVoteChoice().name());

        log.info("Vote submitted successfully: poll={}, voter={}, choice={}",
                poll.getId(), voter != null ? voter.getId() : "anonymous", submitVoteRequest.getVoteChoice());

        return pollVotingMapper.toSuccessResponse(savedVote, poll);
    }

    @Override
    @Transactional
    public SubmitVoteResponse changeVote(SubmitVoteRequest submitVoteRequest, HttpServletRequest request) {
        log.info("Processing vote change for poll ID: {}", submitVoteRequest.getPollId());

        Poll poll = getPollById(submitVoteRequest.getPollId());
        validateVoteSubmission(poll, submitVoteRequest, request);

        User voter = getAuthenticatedUser(request);
        PollVote existingVote = getExistingUserVote(poll, voter);

        VoteChoice previousChoice = existingVote.getVoteChoice();
        pollVotingMapper.updateVoteChoice(existingVote, submitVoteRequest.getVoteChoice());
        PollVote updatedVote = pollVoteRepository.save(existingVote);

        updatePollVoteStatistics(poll, previousChoice.name(), submitVoteRequest.getVoteChoice().name());

        log.info("Vote changed successfully: poll={}, voter={}, {} -> {}",
                poll.getId(), voter.getId(), previousChoice, submitVoteRequest.getVoteChoice());

        return pollVotingMapper.toVoteChangeResponse(existingVote, updatedVote, poll);
    }

    @Override
    @Transactional(readOnly = true)
    public VoteStatusResponse getUserVoteStatus(Long pollId, HttpServletRequest request) {
        log.debug("Getting vote status for poll ID: {}", pollId);

        Poll poll = getPollById(pollId);
        User user = getOptionalUser(request);

        if (user == null) {
            return pollVotingMapper.toVoteStatusResponse(null);
        }

        Optional<PollVote> vote = pollVoteRepository.findByPollAndUser(poll, user);
        return pollVotingMapper.toVoteStatusResponse(vote.orElse(null));
    }

    @Override
    @Transactional
    public void withdrawVote(Long pollId, HttpServletRequest request) {
        log.info("Processing vote withdrawal for poll ID: {}", pollId);

        Poll poll = getPollById(pollId);
        User user = getAuthenticatedUser(request);

        PollVote existingVote = getExistingUserVote(poll, user);

        validateVoteWithdrawal(poll);

        VoteChoice previousChoice = existingVote.getVoteChoice();
        pollVoteRepository.delete(existingVote);
        updatePollVoteStatistics(poll, previousChoice.name(), null);

        log.info("Vote withdrawn successfully: poll={}, voter={}, choice={}",
                poll.getId(), user.getId(), previousChoice);
    }

    @Override
    public void validateVoteSubmission(Poll poll, SubmitVoteRequest submitVoteRequest, HttpServletRequest request) {
        if (submitVoteRequest == null) {
            throw new IllegalArgumentException("Vote request cannot be null");
        }

        if (submitVoteRequest.getPollId() == null) {
            throw new IllegalArgumentException("Poll ID is required");
        }

        if (submitVoteRequest.getVoteChoice() == null) {
            throw new IllegalArgumentException("Vote choice is required");
        }

        if (submitVoteRequest.getIsAnonymous() == null) {
            throw new IllegalArgumentException("Anonymous voting preference must be specified");
        }

        if (!isVotingAllowed(poll)) {
            throw new CustomException("Voting is not allowed for this poll");
        }

        validateVotingMode(poll, submitVoteRequest.getIsAnonymous());
    }

    @Override
    public boolean hasUserVoted(Poll poll, HttpServletRequest request) {
        try {
            User user = getOptionalUser(request);

            if (user != null) {
                return pollVoteRepository.existsByPollAndUser(poll, user);
            } else {
                // Check for anonymous vote using fingerprint
                String fingerprint = generateVoterFingerprint(request);
                return pollVoteRepository.existsByPollAndVoterFingerprint(poll, fingerprint);
            }
        } catch (Exception e) {
            log.error("Error checking if user has voted: {}", e.getMessage());
            return false;
        }
    }

    @Override
    public boolean isVotingAllowed(Poll poll) {
        return poll.isActive();
    }

    @Override
    public String generateVoterFingerprint(HttpServletRequest request) {
        String userAgent = getUserAgent(request);
        String ipAddress = getClientIpAddress(request);
        return (userAgent + ipAddress + "vote").hashCode() + "";
    }

    @Override
    @Transactional
    public void updatePollVoteStatistics(Poll poll, String previousVoteChoice, String newVoteChoice) {
        if (previousVoteChoice != null) {
            decrementVoteCount(poll, VoteChoice.valueOf(previousVoteChoice));
        }

        if (newVoteChoice != null) {
            incrementVoteCount(poll, VoteChoice.valueOf(newVoteChoice));
        }

        long totalVotes = poll.getYesVotes() + poll.getNoVotes() + poll.getNeutralVotes();
        poll.setTotalVotes(totalVotes);

        pollRepository.save(poll);
    }

    @Override
    public void validateVotingMode(Poll poll, boolean isAnonymous) {
        if (isAnonymous && !poll.getAllowAnonymousVoting()) {
            throw new CustomException("Anonymous voting is not allowed for this poll");
        }
    }


    private Poll getPollById(Long pollId) {
        return pollRepository.findById(pollId)
                .orElseThrow(() -> new CustomException("Poll not found with ID: " + pollId));
    }

    private User getAuthenticatedUser(HttpServletRequest request) {
        Long userId = jwtHelperService.getCurrentUserIdFromRequest(request);
        return userRepository.findById(userId)
                .orElseThrow(() -> new CustomException("User not found"));
    }

    private User getOptionalUser(HttpServletRequest request) {
        try {
            return getAuthenticatedUser(request);
        } catch (Exception e) {
            return null;
        }
    }

    private User getVoterIfAuthenticated(Boolean isAnonymous, HttpServletRequest request) {
        if (Boolean.TRUE.equals(isAnonymous)) {
            return null;
        }
        return getAuthenticatedUser(request);
    }

    private void validateNoDuplicateVote(Poll poll, User voter, HttpServletRequest request) {
        if (hasUserVoted(poll, request)) {
            throw new CustomException("You have already voted on this poll");
        }
    }

    private PollVote getExistingUserVote(Poll poll, User user) {
        return pollVoteRepository.findByPollAndUser(poll, user)
                .orElseThrow(() -> new CustomException("No existing vote found for this poll"));
    }

    private void validateVoteWithdrawal(Poll poll) {
        if (!isVotingAllowed(poll)) {
            throw new CustomException("Vote withdrawal is not allowed for this poll");
        }
    }

    private PollVote createVoteEntity(SubmitVoteRequest request, Poll poll, User voter, HttpServletRequest httpRequest) {
        String fingerprint = generateVoterFingerprint(httpRequest);
        String ipAddress = getClientIpAddress(httpRequest);
        String userAgent = getUserAgent(httpRequest);

        return pollVotingMapper.toEntity(request, poll, voter, fingerprint, ipAddress, userAgent);
    }

    private void incrementVoteCount(Poll poll, VoteChoice voteChoice) {
        switch (voteChoice) {
            case YES -> poll.setYesVotes(poll.getYesVotes() + 1);
            case NO -> poll.setNoVotes(poll.getNoVotes() + 1);
            case NEUTRAL -> poll.setNeutralVotes(poll.getNeutralVotes() + 1);
        }
    }

    private void decrementVoteCount(Poll poll, VoteChoice voteChoice) {
        switch (voteChoice) {
            case YES -> poll.setYesVotes(Math.max(0, poll.getYesVotes() - 1));
            case NO -> poll.setNoVotes(Math.max(0, poll.getNoVotes() - 1));
            case NEUTRAL -> poll.setNeutralVotes(Math.max(0, poll.getNeutralVotes() - 1));
        }
    }

    private String getClientIpAddress(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }

        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isEmpty()) {
            return xRealIp;
        }

        return request.getRemoteAddr();
    }

    private String getUserAgent(HttpServletRequest request) {
        String userAgent = request.getHeader("User-Agent");
        return userAgent != null ? userAgent : "Unknown";
    }
}
