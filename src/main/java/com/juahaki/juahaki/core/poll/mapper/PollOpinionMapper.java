package com.juahaki.juahaki.core.poll.mapper;

import com.juahaki.juahaki.core.poll.dto.opinions.OpinionReactionResponse;
import com.juahaki.juahaki.core.poll.dto.opinions.OpinionResponse;
import com.juahaki.juahaki.core.poll.dto.opinions.ReactToOpinionRequest;
import com.juahaki.juahaki.core.poll.dto.opinions.SubmitOpinionRequest;
import com.juahaki.juahaki.core.poll.model.Poll;
import com.juahaki.juahaki.core.poll.model.PollOpinion;
import com.juahaki.juahaki.core.poll.model.PollOpinionAttachment;
import com.juahaki.juahaki.core.poll.model.PollOpinionReaction;
import com.juahaki.juahaki.core.user.model.User;
import com.juahaki.juahaki.shared.enums.ReactionType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Mapper for poll opinion operations.
 * Handles conversion between opinion DTOs and entities, and creates opinion responses.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class PollOpinionMapper {

    /**
     * Convert SubmitOpinionRequest to PollOpinion entity.
     *
     * @param request           the opinion submission request
     * @param poll              the poll the opinion is for
     * @param author            the user submitting the opinion (null for anonymous)
     * @param authorFingerprint unique identifier for anonymous authors
     * @param ipAddress         author's IP address
     * @param userAgent         author's user agent
     * @return new PollOpinion entity
     */
    public PollOpinion toEntity(SubmitOpinionRequest request, Poll poll, User author,
                                String authorFingerprint, String ipAddress, String userAgent) {
        validateOpinionRequest(request, poll);

        return PollOpinion.builder()
                .poll(poll)
                .author(author)
                .content(request.getContent().trim())
                .stance(request.getStance())
                .isAnonymous(request.getIsAnonymous())
                .authorFingerprint(authorFingerprint)
                .ipAddress(ipAddress)
                .userAgent(userAgent)
                .likesCount(0L)
                .dislikesCount(0L)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }

    /**
     * Convert PollOpinion entity to OpinionResponse DTO.
     *
     * @param opinion             the opinion entity
     * @param currentUserReaction current user's reaction to this opinion (null if none)
     * @return opinion response DTO
     */
    public OpinionResponse toResponse(PollOpinion opinion, Boolean currentUserReaction) {
        if (opinion == null) {
            return null;
        }

        OpinionResponse.AuthorInfo authorInfo = buildAuthorInfo(opinion);
        List<OpinionResponse.OpinionAttachmentInfo> attachmentInfos = buildAttachmentInfos(opinion);

        return OpinionResponse.builder()
                .id(opinion.getId())
                .content(opinion.getContent())
                .stance(opinion.getStance())
                .isAnonymous(opinion.getIsAnonymous())
                .author(authorInfo)
                .likesCount(opinion.getLikesCount())
                .dislikesCount(opinion.getDislikesCount())
                .likePercentage(opinion.getLikePercentage())
                .createdAt(opinion.getCreatedAt())
                .updatedAt(opinion.getUpdatedAt())
                .attachments(attachmentInfos)
                .currentUserReaction(currentUserReaction)
                .build();
    }

    /**
     * Convert ReactToOpinionRequest to PollOpinionReaction entity.
     *
     * @param request            the reaction request
     * @param opinion            the opinion being reacted to
     * @param reactor            the user reacting (null for anonymous)
     * @param reactorFingerprint unique identifier for anonymous reactors
     * @param ipAddress          reactor's IP address
     * @param userAgent          reactor's user agent
     * @return new PollOpinionReaction entity
     */
    public PollOpinionReaction toReactionEntity(ReactToOpinionRequest request, PollOpinion opinion,
                                                User reactor, String reactorFingerprint,
                                                String ipAddress, String userAgent) {
        validateReactionRequest(request, opinion);

        return PollOpinionReaction.builder()
                .opinion(opinion)
                .reactor(reactor)
                .reactionType(request.getReactionType())
                .isAnonymous(request.getIsAnonymous())
                .reactorFingerprint(reactorFingerprint)
                .ipAddress(ipAddress)
                .userAgent(userAgent)
                .createdAt(LocalDateTime.now())
                .build();
    }

    /**
     * Create successful opinion reaction response.
     *
     * @param reaction the submitted reaction
     * @param opinion  the updated opinion with current reaction counts
     * @return opinion reaction response
     */
    public OpinionReactionResponse toReactionResponse(PollOpinionReaction reaction, PollOpinion opinion) {
        if (reaction == null || opinion == null) {
            throw new IllegalArgumentException("Reaction and opinion cannot be null");
        }

        return OpinionReactionResponse.builder()
                .reactionType(reaction.getReactionType())
                .likesCount(opinion.getLikesCount())
                .dislikesCount(opinion.getDislikesCount())
                .likePercentage(opinion.getLikePercentage())
                .build();
    }

    /**
     * Create opinion reaction removal response.
     *
     * @param removedReaction the reaction that was removed
     * @param opinion         the updated opinion with current reaction counts
     * @return opinion reaction response
     */
    public OpinionReactionResponse toReactionRemovalResponse(PollOpinionReaction removedReaction, PollOpinion opinion) {
        if (removedReaction == null || opinion == null) {
            throw new IllegalArgumentException("Removed reaction and opinion cannot be null");
        }

        return OpinionReactionResponse.builder()
                .reactionType(null) // No reaction after removal
                .likesCount(opinion.getLikesCount())
                .dislikesCount(opinion.getDislikesCount())
                .likePercentage(opinion.getLikePercentage())
                .build();
    }


    /**
     * Update existing opinion entity with new content from request.
     *
     * @param existingOpinion the opinion to update
     * @param request         the update request
     */
    public void updateOpinion(PollOpinion existingOpinion, SubmitOpinionRequest request) {
        if (existingOpinion == null) {
            throw new IllegalArgumentException("Existing opinion cannot be null");
        }

        if (request == null) {
            throw new IllegalArgumentException("Update request cannot be null");
        }

        if (request.getContent() != null && !request.getContent().trim().isEmpty()) {
            existingOpinion.setContent(request.getContent().trim());
        }

        if (request.getStance() != null) {
            existingOpinion.setStance(request.getStance());
        }

        existingOpinion.setUpdatedAt(LocalDateTime.now());
    }

    /**
     * Update existing reaction entity with new reaction type.
     *
     * @param existingReaction the reaction to update
     * @param newReactionType  the new reaction type
     */
    public void updateReaction(PollOpinionReaction existingReaction, ReactionType newReactionType) {
        if (existingReaction == null) {
            throw new IllegalArgumentException("Existing reaction cannot be null");
        }

        if (newReactionType == null) {
            throw new IllegalArgumentException("New reaction type cannot be null");
        }

        existingReaction.setReactionType(newReactionType);
    }

    /**
     * Build author information for opinion response.
     * Handles anonymous vs registered author display.
     *
     * @param opinion the opinion entity
     * @return author info DTO
     */
    private OpinionResponse.AuthorInfo buildAuthorInfo(PollOpinion opinion) {
        if (opinion.getIsAnonymous() || opinion.getAuthor() == null) {
            return OpinionResponse.AuthorInfo.builder()
                    .username("Anonymous")
                    .firstName("Anonymous")
                    .isRegistered(false)
                    .build();
        }

        return OpinionResponse.AuthorInfo.builder()
                .username(opinion.getAuthor().getUsername())
                .firstName(opinion.getAuthor().getFirstName())
                .isRegistered(true)
                .build();
    }

    /**
     * Build attachment information list for opinion response.
     *
     * @param opinion the opinion entity
     * @return list of attachment info DTOs
     */
    private List<OpinionResponse.OpinionAttachmentInfo> buildAttachmentInfos(PollOpinion opinion) {
        if (opinion.getAttachments() == null || opinion.getAttachments().isEmpty()) {
            return List.of();
        }

        return opinion.getAttachments().stream()
                .map(this::mapAttachmentToInfo)
                .collect(Collectors.toList());
    }

    /**
     * Map PollOpinionAttachment entity to AttachmentInfo DTO.
     *
     * @param attachment the attachment entity
     * @return attachment info DTO
     */
    private OpinionResponse.OpinionAttachmentInfo mapAttachmentToInfo(PollOpinionAttachment attachment) {
        if (attachment == null) {
            return null;
        }

        return OpinionResponse.OpinionAttachmentInfo.builder()
                .id(attachment.getId())
                .fileName(attachment.getFileName())
                .fileUrl(attachment.getFileUrl())
                .attachmentType(attachment.getAttachmentType().name())
                .fileSize(attachment.getFileSize())
                .build();
    }

    /**
     * Validate opinion submission request.
     *
     * @param request the opinion request to validate
     * @param poll    the poll the opinion is for
     */
    private void validateOpinionRequest(SubmitOpinionRequest request, Poll poll) {
        if (request == null) {
            throw new IllegalArgumentException("Opinion request cannot be null");
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

        if (request.getContent() == null || request.getContent().trim().isEmpty()) {
            throw new IllegalArgumentException("Opinion content is required");
        }

        if (request.getStance() == null) {
            throw new IllegalArgumentException("Opinion stance is required");
        }

        if (request.getIsAnonymous() == null) {
            throw new IllegalArgumentException("Anonymous preference must be specified");
        }
    }

    /**
     * Validate reaction request.
     *
     * @param request the reaction request to validate
     * @param opinion the opinion being reacted to
     */
    private void validateReactionRequest(ReactToOpinionRequest request, PollOpinion opinion) {
        if (request == null) {
            throw new IllegalArgumentException("Reaction request cannot be null");
        }

        if (opinion == null) {
            throw new IllegalArgumentException("Opinion cannot be null");
        }

        if (request.getOpinionId() == null) {
            throw new IllegalArgumentException("Opinion ID is required");
        }

        if (!request.getOpinionId().equals(opinion.getId())) {
            throw new IllegalArgumentException("Opinion ID mismatch");
        }

        if (request.getReactionType() == null) {
            throw new IllegalArgumentException("Reaction type is required");
        }

        if (request.getIsAnonymous() == null) {
            throw new IllegalArgumentException("Anonymous preference must be specified");
        }
    }

    /**
     * Create a response for successful opinion change reaction.
     *
     * @param originalReaction the original reaction
     * @param updatedReaction  the updated reaction
     * @param opinion          the opinion with updated counts
     * @return reaction change response
     */
    public OpinionReactionResponse toReactionChangeResponse(PollOpinionReaction originalReaction,
                                                            PollOpinionReaction updatedReaction,
                                                            PollOpinion opinion) {
        if (originalReaction == null || updatedReaction == null || opinion == null) {
            throw new IllegalArgumentException("Original reaction, updated reaction, and opinion cannot be null");
        }

        return OpinionReactionResponse.builder()
                .reactionType(updatedReaction.getReactionType())
                .likesCount(opinion.getLikesCount())
                .dislikesCount(opinion.getDislikesCount())
                .likePercentage(opinion.getLikePercentage())
                .build();
    }
}