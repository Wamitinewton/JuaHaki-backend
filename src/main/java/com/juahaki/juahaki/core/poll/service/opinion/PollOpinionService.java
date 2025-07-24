package com.juahaki.juahaki.core.poll.service.opinion;

import com.juahaki.juahaki.core.poll.dto.filters.OpinionFilterRequest;
import com.juahaki.juahaki.core.poll.dto.opinions.OpinionReactionResponse;
import com.juahaki.juahaki.core.poll.dto.opinions.OpinionResponse;
import com.juahaki.juahaki.core.poll.dto.opinions.ReactToOpinionRequest;
import com.juahaki.juahaki.core.poll.dto.opinions.SubmitOpinionRequest;
import com.juahaki.juahaki.core.poll.mapper.PollFilterMapper;
import com.juahaki.juahaki.core.poll.mapper.PollOpinionMapper;
import com.juahaki.juahaki.core.poll.model.Poll;
import com.juahaki.juahaki.core.poll.model.PollOpinion;
import com.juahaki.juahaki.core.poll.model.PollOpinionReaction;
import com.juahaki.juahaki.core.poll.repository.PollOpinionReactionRepository;
import com.juahaki.juahaki.core.poll.repository.PollOpinionRepository;
import com.juahaki.juahaki.core.poll.repository.PollRepository;
import com.juahaki.juahaki.core.poll.service.attachment.IPollAttachmentService;
import com.juahaki.juahaki.core.user.model.User;
import com.juahaki.juahaki.core.user.repository.UserRepository;
import com.juahaki.juahaki.shared.dto.response.PageResponse;
import com.juahaki.juahaki.shared.enums.PollStatus;
import com.juahaki.juahaki.shared.enums.ReactionType;
import com.juahaki.juahaki.shared.enums.Role;
import com.juahaki.juahaki.shared.exception.CustomException;
import com.juahaki.juahaki.shared.utils.jwt.JwtHelperService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class PollOpinionService implements IPollOpinionService {

    private final PollRepository pollRepository;
    private final PollOpinionRepository pollOpinionRepository;
    private final PollOpinionReactionRepository pollOpinionReactionRepository;
    private final UserRepository userRepository;
    private final PollOpinionMapper pollOpinionMapper;
    private final PollFilterMapper pollFilterMapper;
    private final IPollAttachmentService pollAttachmentService;
    private final JwtHelperService jwtHelperService;


    @Override
    @Transactional
    public OpinionResponse submitOpinion(SubmitOpinionRequest submitOpinionRequest, HttpServletRequest request) {
        log.info("Submitting opinion for poll ID: {}", submitOpinionRequest.getPollId());

        // Step 1: Get poll and validate
        Poll poll = getPollById(submitOpinionRequest.getPollId());
        validateOpinionSubmission(poll, submitOpinionRequest, request);

        // Step 2: Get user (null for anonymous)
        User author = getAuthorIfAuthenticated(submitOpinionRequest.getIsAnonymous(), request);

        // Step 3: Check for existing opinion
        validateNoDuplicateOpinion(poll, author, request);

        // Step 4: Create opinion entity
        String fingerprint = generateFingerprint(request);
        String ipAddress = getClientIpAddress(request);
        String userAgent = getUserAgent(request);

        PollOpinion opinion = pollOpinionMapper.toEntity(
                submitOpinionRequest, poll, author, fingerprint, ipAddress, userAgent);

        // Step 5: Save opinion
        PollOpinion savedOpinion = pollOpinionRepository.save(opinion);

        // Step 6: Handle attachments if present
        if (submitOpinionRequest.getAttachments() != null && !submitOpinionRequest.getAttachments().isEmpty()) {
            pollAttachmentService.uploadOpinionAttachments(savedOpinion, submitOpinionRequest.getAttachments());
        }

        // Step 7: Update poll statistics
        updatePollOpinionCount(poll);

        log.info("Opinion submitted successfully with ID: {}", savedOpinion.getId());
        return pollOpinionMapper.toResponse(savedOpinion, null);
    }

    @Override
    @Transactional
    public OpinionResponse updateOpinion(Long opinionId, SubmitOpinionRequest submitOpinionRequest, HttpServletRequest request) {
        log.info("Updating opinion with ID: {}", opinionId);

        PollOpinion existingOpinion = getOpinionById(opinionId);

        validateOpinionModificationPermissions(existingOpinion, request);

        validateOpinionUpdateEligibility(existingOpinion);

        if (submitOpinionRequest.getAttachments() != null && !submitOpinionRequest.getAttachments().isEmpty()) {
            pollAttachmentService.validateOpinionAttachments(submitOpinionRequest.getAttachments());
        }

        handleOpinionAttachmentUpdates(existingOpinion, submitOpinionRequest);

        pollOpinionMapper.updateOpinion(existingOpinion, submitOpinionRequest);

        PollOpinion updatedOpinion = pollOpinionRepository.save(existingOpinion);

        log.info("Opinion updated successfully with ID: {}", updatedOpinion.getId());
        return pollOpinionMapper.toResponse(updatedOpinion, null);
    }

    @Override
    @Transactional
    public void deleteOpinion(Long opinionId, HttpServletRequest request) {
        log.info("Deleting opinion with ID: {}", opinionId);

        PollOpinion opinion = getOpinionById(opinionId);

        validateOpinionModificationPermissions(opinion, request);

        pollAttachmentService.deleteAllOpinionAttachments(opinion);

        pollOpinionRepository.delete(opinion);

        updatePollOpinionCount(opinion.getPoll());

        log.info("Opinion deleted successfully with ID: {}", opinionId);
    }

    @Override
    @Transactional
    public OpinionReactionResponse reactToOpinion(ReactToOpinionRequest reactRequest, HttpServletRequest request) {
        log.info("Reacting to opinion with ID: {}", reactRequest.getOpinionId());

        PollOpinion opinion = getOpinionById(reactRequest.getOpinionId());
        validateReactionRequest(opinion, reactRequest, request);

        User reactor = getAuthorIfAuthenticated(reactRequest.getIsAnonymous(), request);

        Optional<PollOpinionReaction> existingReaction = findExistingReaction(opinion, reactor, request);

        if (existingReaction.isPresent()) {
            return handleExistingReaction(existingReaction.get(), reactRequest, opinion);
        } else {
            return handleNewReaction(opinion, reactor, reactRequest, request);
        }
    }

    @Override
    @Transactional
    public OpinionReactionResponse removeReaction(Long opinionId, HttpServletRequest request) {
        log.info("Removing reaction from opinion with ID: {}", opinionId);

        // Step 1: Get opinion
        PollOpinion opinion = getOpinionById(opinionId);

        // Step 2: Find existing reaction
        User reactor = getOptionalUser(request);
        Optional<PollOpinionReaction> existingReaction = findExistingReaction(opinion, reactor, request);

        if (existingReaction.isEmpty()) {
            throw new CustomException("No reaction found to remove");
        }

        PollOpinionReaction reaction = existingReaction.get();
        pollOpinionReactionRepository.delete(reaction);

        updateOpinionReactionStatistics(opinion, reaction.getReactionType().name(), null);

        log.info("Reaction removed successfully from opinion ID: {}", opinionId);
        return pollOpinionMapper.toReactionRemovalResponse(reaction, opinion);
    }


    @Override
    @Transactional(readOnly = true)
    public PageResponse<OpinionResponse> getOpinionsByPollId(Long pollId, Pageable pageable, HttpServletRequest request) {
        log.debug("Getting opinions for poll ID: {} with pagination", pollId);

        Poll poll = getPollById(pollId);

        User currentUser = getOptionalUser(request);

        Page<PollOpinion> opinionsPage = pollOpinionRepository.findByPollOrderByCreatedAtDesc(poll, pageable);

        List<OpinionResponse> opinionResponses = opinionsPage.getContent().stream()
                .map(opinion -> {
                    Boolean userReaction = getUserReactionForOpinion(opinion, currentUser, request);
                    return pollOpinionMapper.toResponse(opinion, userReaction);
                })
                .toList();

        return PageResponse.of(opinionsPage, opinionResponses);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<OpinionResponse> getFilteredOpinions(OpinionFilterRequest filterRequest, Pageable pageable, HttpServletRequest request) {
        log.debug("Getting filtered opinions with filter: {}", filterRequest);

        User currentUser = getOptionalUser(request);
        Specification<PollOpinion> spec = pollFilterMapper.createOpinionSpecification(filterRequest, currentUser);

        Page<PollOpinion> opinions = pollOpinionRepository.findAll(spec, pageable);

        return opinions.map(opinion -> {
            Boolean userReaction = getUserReactionForOpinion(opinion, currentUser, request);
            return pollOpinionMapper.toResponse(opinion, userReaction);
        });
    }

    @Override
    @Transactional(readOnly = true)
    public List<OpinionResponse> getTopOpinions(Long pollId, int limit, HttpServletRequest request) {
        log.debug("Getting top {} opinions for poll ID: {}", limit, pollId);

        Poll poll = getPollById(pollId);
        User currentUser = getOptionalUser(request);

        List<PollOpinion> topOpinions = pollOpinionRepository.findTopOpinionsByLikes(
                poll, PageRequest.of(0, limit)).getContent();

        return topOpinions.stream()
                .map(opinion -> {
                    Boolean userReaction = getUserReactionForOpinion(opinion, currentUser, request);
                    return pollOpinionMapper.toResponse(opinion, userReaction);
                })
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<OpinionResponse> getRecentOpinions(Long pollId, int limit, HttpServletRequest request) {
        log.debug("Getting recent {} opinions for poll ID: {}", limit, pollId);

        Poll poll = getPollById(pollId);
        User currentUser = getOptionalUser(request);

        List<PollOpinion> recentOpinions = pollOpinionRepository.findRecentOpinions(
                poll, PageRequest.of(0, limit)).getContent();

        return recentOpinions.stream()
                .map(opinion -> {
                    Boolean userReaction = getUserReactionForOpinion(opinion, currentUser, request);
                    return pollOpinionMapper.toResponse(opinion, userReaction);
                })
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public Page<OpinionResponse> getUserOpinions(Pageable pageable, HttpServletRequest request) {
        log.debug("Getting user opinions");

        User currentUser = getAuthenticatedUser(request);
        Page<PollOpinion> opinions = pollOpinionRepository.findByAuthorOrderByCreatedAtDesc(currentUser, pageable);

        return opinions.map(opinion -> pollOpinionMapper.toResponse(opinion, null));
    }

    @Override
    public void validateOpinionSubmission(Poll poll, SubmitOpinionRequest submitOpinionRequest, HttpServletRequest request) {
        // Check if poll allows opinions
        if (!areOpinionsAllowed(poll)) {
            throw new CustomException("This poll does not allow opinions");
        }

        // Validate request content
        if (submitOpinionRequest.getContent() == null || submitOpinionRequest.getContent().trim().isEmpty()) {
            throw new IllegalArgumentException("Opinion content is required");
        }

        if (submitOpinionRequest.getStance() == null) {
            throw new IllegalArgumentException("Opinion stance is required");
        }

        // Validate attachments if present
        if (submitOpinionRequest.getAttachments() != null && !submitOpinionRequest.getAttachments().isEmpty()) {
            pollAttachmentService.validateOpinionAttachments(submitOpinionRequest.getAttachments());
        }
    }

    @Override
    public void validateReactionRequest(PollOpinion opinion, ReactToOpinionRequest reactRequest, HttpServletRequest request) {
        if (reactRequest.getReactionType() == null) {
            throw new IllegalArgumentException("Reaction type is required");
        }

        if (!areOpinionsAllowed(opinion.getPoll())) {
            throw new CustomException("Reactions are not allowed for this poll");
        }
    }

    @Override
    public boolean canModifyOpinion(PollOpinion opinion, HttpServletRequest request) {
        try {
            Long currentUserId = jwtHelperService.getCurrentUserIdFromRequest(request);
            Role currentUserRole = jwtHelperService.getCurrentUserRoleFromRequest(request);

            // Admin can modify any opinion
            if (currentUserRole == Role.ADMIN) {
                return true;
            }

            // Author can modify their own opinion
            return opinion.getAuthor() != null && opinion.getAuthor().getId().equals(currentUserId);
        } catch (Exception e) {
            log.error("Error checking opinion modification permissions: {}", e.getMessage());
            return false;
        }
    }

    @Override
    public boolean areOpinionsAllowed(Poll poll) {
        return poll.getAllowOpinions() &&
                poll.getStatus() == PollStatus.ACTIVE &&
                !poll.isExpired();
    }

    @Override
    public void updateOpinionReactionStatistics(PollOpinion opinion, String previousReactionType, String newReactionType) {
        // Decrement previous reaction count
        if (previousReactionType != null) {
            if ("LIKE".equals(previousReactionType)) {
                opinion.setLikesCount(Math.max(0, opinion.getLikesCount() - 1));
            } else if ("DISLIKE".equals(previousReactionType)) {
                opinion.setDislikesCount(Math.max(0, opinion.getDislikesCount() - 1));
            }
        }

        // Increment new reaction count
        if (newReactionType != null) {
            if ("LIKE".equals(newReactionType)) {
                opinion.setLikesCount(opinion.getLikesCount() + 1);
            } else if ("DISLIKE".equals(newReactionType)) {
                opinion.setDislikesCount(opinion.getDislikesCount() + 1);
            }
        }

        pollOpinionRepository.save(opinion);
    }

    private Poll getPollById(Long pollId) {
        return pollRepository.findById(pollId)
                .orElseThrow(() -> new CustomException("Poll not found with ID: " + pollId));
    }

    private PollOpinion getOpinionById(Long opinionId) {
        return pollOpinionRepository.findById(opinionId)
                .orElseThrow(() -> new CustomException("Opinion not found with ID: " + opinionId));
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

    private User getAuthorIfAuthenticated(Boolean isAnonymous, HttpServletRequest request) {
        if (Boolean.TRUE.equals(isAnonymous)) {
            return null;
        }
        return getAuthenticatedUser(request);
    }

    private void validateNoDuplicateOpinion(Poll poll, User author, HttpServletRequest request) {
        if (author != null) {
            boolean hasExistingOpinion = pollOpinionRepository.existsByPollAndAuthor(poll, author);
            if (hasExistingOpinion) {
                throw new CustomException("You have already submitted an opinion for this poll");
            }
        }
    }

    private void validateOpinionModificationPermissions(PollOpinion opinion, HttpServletRequest request) {
        if (!canModifyOpinion(opinion, request)) {
            throw new CustomException("You don't have permission to modify this opinion");
        }
    }

    private void validateOpinionUpdateEligibility(PollOpinion opinion) {
        if (!areOpinionsAllowed(opinion.getPoll())) {
            throw new CustomException("Opinion updates are not allowed for this poll");
        }
    }

    private Optional<PollOpinionReaction> findExistingReaction(PollOpinion opinion, User reactor, HttpServletRequest request) {
        if (reactor != null) {
            return pollOpinionReactionRepository.findByOpinionAndReactor(opinion, reactor);
        } else {
            String fingerprint = generateFingerprint(request);
            return pollOpinionReactionRepository.findAll().stream()
                    .filter(r -> r.getOpinion().equals(opinion) && fingerprint.equals(r.getReactorFingerprint()))
                    .findFirst();
        }
    }

    private OpinionReactionResponse handleExistingReaction(PollOpinionReaction existingReaction,
                                                           ReactToOpinionRequest reactRequest,
                                                           PollOpinion opinion) {
        if (existingReaction.getReactionType() == reactRequest.getReactionType()) {
            // Same reaction remove it
            pollOpinionReactionRepository.delete(existingReaction);
            updateOpinionReactionStatistics(opinion, existingReaction.getReactionType().name(), null);
            return pollOpinionMapper.toReactionRemovalResponse(existingReaction, opinion);
        } else {

            // Different reaction - update it
            ReactionType previousType = existingReaction.getReactionType();
            pollOpinionMapper.updateReaction(existingReaction, reactRequest.getReactionType());
            pollOpinionReactionRepository.save(existingReaction);
            updateOpinionReactionStatistics(opinion, previousType.name(), reactRequest.getReactionType().name());
            return pollOpinionMapper.toReactionChangeResponse(existingReaction, existingReaction, opinion);
        }
    }

    private OpinionReactionResponse handleNewReaction(PollOpinion opinion, User reactor,
                                                      ReactToOpinionRequest reactRequest,
                                                      HttpServletRequest request) {
        String fingerPrint = generateFingerprint(request);
        String ipAddress = getClientIpAddress(request);
        String userAgent = getUserAgent(request);

        PollOpinionReaction newReaction = pollOpinionMapper.toReactionEntity(
                reactRequest, opinion, reactor, fingerPrint, ipAddress, userAgent
        );

        pollOpinionReactionRepository.save(newReaction);
        updateOpinionReactionStatistics(opinion, null, reactRequest.getReactionType().name());

        return pollOpinionMapper.toReactionResponse(newReaction, opinion);
    }

    private void updatePollOpinionCount(Poll poll) {
        long opinionCount = pollOpinionRepository.countByPoll(poll);
        poll.setTotalOpinions(opinionCount);
        pollRepository.save(poll);
    }

    private String generateFingerprint(HttpServletRequest request) {
        String userAgent = getUserAgent(request);
        String ipAddress = getClientIpAddress(request);
        return (userAgent + ipAddress).hashCode() + "";
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

    private Boolean getUserReactionForOpinion(PollOpinion opinion, User currentUser, HttpServletRequest request) {
        if (currentUser != null) {
            Optional<PollOpinionReaction> reaction = pollOpinionReactionRepository.findByOpinionAndReactor(opinion, currentUser);
            return reaction.map(r -> r.getReactionType() == ReactionType.LIKE).orElse(null);
        } else {
            try {
                String fingerPrint = generateFingerprint(request);

                Optional<PollOpinionReaction> reaction = pollOpinionReactionRepository.findAll().stream()
                        .filter(r -> r.getOpinion().equals(opinion) && fingerPrint.equals(r.getReactorFingerprint()))
                        .findFirst();

                return reaction.map(r -> r.getReactionType() == ReactionType.LIKE).orElse(null);
            } catch (Exception e) {
                return null;
            }
        }
    }


    private void handleOpinionAttachmentUpdates(PollOpinion existingOpinion, SubmitOpinionRequest submitOpinionRequest) {
        boolean hasNewAttachments = submitOpinionRequest.getAttachments() != null &&
                !submitOpinionRequest.getAttachments().isEmpty();

        if (hasNewAttachments) {
            log.info("Updating attachments for opinion ID: {}", existingOpinion.getId());

            try {
                int deletedCount = pollAttachmentService.deleteAllOpinionAttachments(existingOpinion);
                log.info("Deleted {} existing attachments for opinion ID: {}", deletedCount, existingOpinion.getId());

                List<Object> uploadedAttachments = pollAttachmentService.uploadOpinionAttachments(
                        existingOpinion, submitOpinionRequest.getAttachments());
                log.info("Uploaded {} new attachments for opinion ID: {}",
                        uploadedAttachments.size(), existingOpinion.getId());

            } catch (Exception e) {
                log.error("Error updating attachments for opinion ID: {}", existingOpinion.getId(), e);
                throw new CustomException("Failed to update opinion attachments: " + e.getMessage());
            }
        } else {
            log.debug("No new attachments provided for opinion ID: {}", existingOpinion.getId());
        }
    }
}
