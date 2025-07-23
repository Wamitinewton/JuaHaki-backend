package com.juahaki.juahaki.core.poll.mapper;

import com.juahaki.juahaki.core.poll.dto.creation.CreatePollRequest;
import com.juahaki.juahaki.core.poll.dto.creation.CreatePollResponse;
import com.juahaki.juahaki.core.poll.dto.creation.UpdatePollRequest;
import com.juahaki.juahaki.core.poll.model.Poll;
import com.juahaki.juahaki.core.poll.model.PollAttachment;
import com.juahaki.juahaki.core.user.model.User;
import com.juahaki.juahaki.shared.enums.PollStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Mapper for poll creation and management operations.
 * Handles conversion between DTOs and entities for poll creation, updates, and responses.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class PollCreationMapper {

    /**
     * Convert CreatePollRequest to Poll entity for persistence.
     *
     * @param request the creation request
     * @param creator the user creating the poll
     * @return new Poll entity
     */
    public Poll toEntity(CreatePollRequest request, User creator) {
        validateCreateRequest(request, creator);

        return Poll.builder()
                .title(request.getTitle().trim())
                .description(request.getDescription() != null ? request.getDescription().trim() : null)
                .category(request.getCategory())
                .creator(creator)
                .startDate(request.getStartDate())
                .endDate(request.getEndDate())
                .allowAnonymousVoting(request.getAllowAnonymousVoting())
                .allowOpinions(request.getAllowOpinions())
                .status(PollStatus.DRAFT) // New polls start as draft
                .totalVotes(0L)
                .yesVotes(0L)
                .noVotes(0L)
                .neutralVotes(0L)
                .totalOpinions(0L)
                .build();
    }

    /**
     * Convert Poll entity to CreatePollResponse for API response.
     *
     * @param poll the poll entity
     * @return response DTO
     */
    public CreatePollResponse toResponse(Poll poll) {
        if (poll == null) {
            return null;
        }

        List<CreatePollResponse.AttachmentInfo> attachmentInfos = poll.getAttachments() != null
                ? poll.getAttachments().stream()
                .map(this::mapAttachmentToInfo)
                .collect(Collectors.toList())
                : List.of();

        return CreatePollResponse.builder()
                .pollId(poll.getId())
                .title(poll.getTitle())
                .description(poll.getDescription())
                .category(poll.getCategory())
                .startDate(poll.getStartDate())
                .endDate(poll.getEndDate())
                .allowAnonymousVoting(poll.getAllowAnonymousVoting())
                .allowOpinions(poll.getAllowOpinions())
                .createDate(poll.getCreatedAt())
                .creatorUsername(poll.getCreator() != null ? poll.getCreator().getUsername() : "Unknown")
                .attachments(attachmentInfos)
                .build();
    }

    /**
     * Update existing Poll entity with data from UpdatePollRequest.
     *
     * @param existingPoll the poll to update
     * @param request the update request
     */
    public void updateEntity(Poll existingPoll, UpdatePollRequest request) {
        validateUpdateRequest(existingPoll, request);

        if (request.getTitle() != null && !request.getTitle().trim().isEmpty()) {
            existingPoll.setTitle(request.getTitle().trim());
        }

        if (request.getDescription() != null) {
            existingPoll.setDescription(request.getDescription().trim());
        }

        if (request.getCategory() != null) {
            existingPoll.setCategory(request.getCategory());
        }

        if (request.getStartDate() != null) {
            existingPoll.setStartDate(request.getStartDate());
        }

        if (request.getEndDate() != null) {
            existingPoll.setEndDate(request.getEndDate());
        }

        if (request.getAllowAnonymousVoting() != null) {
            existingPoll.setAllowAnonymousVoting(request.getAllowAnonymousVoting());
        }

        if (request.getAllowOpinions() != null) {
            existingPoll.setAllowOpinions(request.getAllowOpinions());
        }

        existingPoll.setUpdatedAt(LocalDateTime.now());
    }

    /**
     * Map PollAttachment entity to AttachmentInfo DTO.
     *
     * @param attachment the attachment entity
     * @return attachment info DTO
     */
    private CreatePollResponse.AttachmentInfo mapAttachmentToInfo(PollAttachment attachment) {
        if (attachment == null) {
            return null;
        }

        return CreatePollResponse.AttachmentInfo.builder()
                .id(attachment.getId())
                .fileName(attachment.getFilaName())
                .fileUrl(attachment.getFileUrl())
                .attachmentType(attachment.getAttachmentType().name())
                .fileSize(attachment.getFileSize())
                .build();
    }

    /**
     * Validate create poll request.
     *
     * @param request the request to validate
     * @param creator the user creating the poll
     */
    private void validateCreateRequest(CreatePollRequest request, User creator) {
        if (request == null) {
            throw new IllegalArgumentException("Create poll request cannot be null");
        }

        if (creator == null) {
            throw new IllegalArgumentException("Poll creator cannot be null");
        }

        if (request.getTitle() == null || request.getTitle().trim().isEmpty()) {
            throw new IllegalArgumentException("Poll title is required");
        }

        if (request.getCategory() == null) {
            throw new IllegalArgumentException("Poll category is required");
        }

        if (request.getStartDate() == null || request.getEndDate() == null) {
            throw new IllegalArgumentException("Start date and end date are required");
        }

        if (request.getEndDate().isBefore(request.getStartDate())) {
            throw new IllegalArgumentException("End date must be after start date");
        }

        if (request.getStartDate().isBefore(LocalDateTime.now())) {
            throw new IllegalArgumentException("Start date cannot be in the past");
        }
    }

    /**
     * Validate update poll request.
     *
     * @param existingPoll the existing poll
     * @param request the update request
     */
    private void validateUpdateRequest(Poll existingPoll, UpdatePollRequest request) {
        if (existingPoll == null) {
            throw new IllegalArgumentException("Existing poll cannot be null");
        }

        if (request == null) {
            throw new IllegalArgumentException("Update request cannot be null");
        }

        // Check if poll can be updated based on its status
        if (existingPoll.getStatus() == PollStatus.CLOSED ||
                existingPoll.getStatus() == PollStatus.ARCHIVED) {
            throw new IllegalStateException("Cannot update a closed or archived poll");
        }

        // Validate date updates
        if (request.getStartDate() != null && request.getEndDate() != null) {
            if (request.getEndDate().isBefore(request.getStartDate())) {
                throw new IllegalArgumentException("End date must be after start date");
            }
        } else if (request.getEndDate() != null) {
            if (request.getEndDate().isBefore(existingPoll.getStartDate())) {
                throw new IllegalArgumentException("End date must be after current start date");
            }
        } else if (request.getStartDate() != null) {
            if (existingPoll.getEndDate().isBefore(request.getStartDate())) {
                throw new IllegalArgumentException("Start date must be before current end date");
            }
        }

        // If poll is active and has votes, restrict certain updates
        if (existingPoll.getStatus() == PollStatus.ACTIVE && existingPoll.getTotalVotes() > 0) {
            if (request.getAllowAnonymousVoting() != null &&
                    !request.getAllowAnonymousVoting().equals(existingPoll.getAllowAnonymousVoting())) {
                throw new IllegalStateException("Cannot change anonymous voting setting after votes have been cast");
            }
        }
    }

    /**
     * @param poll the poll entity
     * @param message success message
     * @return simplified response
     */
    public CreatePollResponse toMinimalResponse(Poll poll, String message) {
        if (poll == null) {
            return null;
        }

        return CreatePollResponse.builder()
                .pollId(poll.getId())
                .title(poll.getTitle())
                .category(poll.getCategory())
                .createDate(poll.getCreatedAt())
                .creatorUsername(poll.getCreator() != null ? poll.getCreator().getUsername() : "Unknown")
                .build();
    }
}
