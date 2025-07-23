package com.juahaki.juahaki.core.poll.service.creation;

import com.juahaki.juahaki.core.poll.dto.creation.CreatePollRequest;
import com.juahaki.juahaki.core.poll.dto.creation.CreatePollResponse;
import com.juahaki.juahaki.core.poll.dto.creation.UpdatePollRequest;
import com.juahaki.juahaki.core.poll.mapper.PollCreationMapper;
import com.juahaki.juahaki.core.poll.model.Poll;
import com.juahaki.juahaki.core.poll.repository.PollRepository;
import com.juahaki.juahaki.core.poll.service.attachment.IPollAttachmentService;
import com.juahaki.juahaki.core.user.model.User;
import com.juahaki.juahaki.core.user.repository.UserRepository;
import com.juahaki.juahaki.shared.enums.PollStatus;
import com.juahaki.juahaki.shared.enums.Role;
import com.juahaki.juahaki.shared.exception.CustomException;
import com.juahaki.juahaki.shared.utils.jwt.JwtHelperService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class PollCreationService implements IPollCreationService {

    private final PollRepository pollRepository;
    private final UserRepository userRepository;
    private final PollCreationMapper pollCreationMapper;
    private final IPollAttachmentService pollAttachmentService;
    private final JwtHelperService jwtHelperService;

    @Override
    public CreatePollResponse createPoll(CreatePollRequest createRequest, HttpServletRequest request) {
        log.info("Creating new poll: {}", createRequest.getTitle());

        validateCreatePollRequest(createRequest, request);

        User creator = getAuthenticatedUser(request);

        Poll poll = pollCreationMapper.toEntity(createRequest, creator);

        Poll savedPoll = pollRepository.save(poll);

        if (createRequest.getAttachments() != null && !createRequest.getAttachments().isEmpty()) {
            pollAttachmentService.uploadPollAttachments(savedPoll, createRequest.getAttachments());
        }

        log.info("Poll created successfully with ID: {}", savedPoll.getId());
        return pollCreationMapper.toResponse(savedPoll);
    }

    @Override
    public CreatePollResponse updatePoll(Long pollId, UpdatePollRequest updateRequest, HttpServletRequest request) {
        log.info("Updating poll with ID: {}", pollId);

        Poll existingPoll = getPollById(pollId);

        validateUpdatePollRequest(existingPoll, updateRequest, request);

        validateUserPermissions(existingPoll, request);

        pollCreationMapper.updateEntity(existingPoll, updateRequest);

        Poll updatedPoll = pollRepository.save(existingPoll);

        log.info("Poll updated successfully with ID: {}", updatedPoll.getId());
        return pollCreationMapper.toResponse(updatedPoll);
    }

    @Override
    public CreatePollResponse activatePoll(Long pollId, HttpServletRequest request) {
        log.info("Activating poll with ID: {}", pollId);

        Poll poll = getPollById(pollId);
        validatePollForActivation(poll);

        validateUserPermissions(poll, request);

        poll.setStatus(PollStatus.ACTIVE);
        Poll activatedPoll = pollRepository.save(poll);

        log.info("Poll activated successfully with ID: {}", activatedPoll.getId());
        return pollCreationMapper.toResponse(activatedPoll);
    }

    @Override
    public CreatePollResponse closePoll(Long pollId, HttpServletRequest request) {
        log.info("Closing poll with ID: {}", pollId);

        Poll poll = getPollById(pollId);
        validatePollForClosure(poll);

        validateUserPermissions(poll, request);

        poll.setStatus(PollStatus.CLOSED);
        Poll closedPoll = pollRepository.save(poll);

        log.info("Poll closed successfully with ID: {}", closedPoll.getId());
        return pollCreationMapper.toResponse(closedPoll);
    }

    @Override
    public void suspendPoll(Long pollId, HttpServletRequest request) {
        log.info("Suspending poll with ID: {}", pollId);

        Poll poll = getPollById(pollId);

        validateAdminPermissions(request);

        poll.setStatus(PollStatus.SUSPENDED);
        pollRepository.save(poll);

        log.info("Poll suspended successfully with ID: {}", pollId);
    }

    @Override
    public void deletePoll(Long pollId, HttpServletRequest request) {
        log.info("Deleting poll with ID: {}", pollId);

        Poll poll = getPollById(pollId);

        validatePollForDeletion(poll);

        validateAdminPermissions(request);

        pollAttachmentService.deleteAllPollAttachments(poll);

        pollRepository.delete(poll);

        log.info("Poll with ID: {} deleted successfully", pollId);
    }

    @Override
    public void archivePoll(Long pollId, HttpServletRequest request) {
        log.info("Archiving poll with ID: {}", pollId);

        // step 1: Get poll
        Poll poll = getPollById(pollId);

        // Step 2: Check permissions
        validateUserPermissions(poll, request);

        // step 3: Archive poll
        poll.setStatus(PollStatus.ARCHIVED);
        pollRepository.save(poll);

        log.info("Archived poll with ID: {}", pollId);

    }

    @Override
    public void validateCreatePollRequest(CreatePollRequest createRequest, HttpServletRequest request) {
        if (createRequest == null) {
            throw new IllegalArgumentException("Poll creation request is required");
        }

        if (createRequest.getTitle() == null || createRequest.getTitle().trim().isEmpty()) {
            throw new IllegalArgumentException("Poll title is required");
        }

        if (createRequest.getCategory() == null) {
            throw new IllegalArgumentException("Poll category is required");
        }

        if (createRequest.getStartDate() == null || createRequest.getEndDate() == null) {
            throw new IllegalArgumentException("Start date and end date are required");
        }

        if (createRequest.getEndDate().isBefore(createRequest.getStartDate())) {
            throw new IllegalArgumentException("End date must be after start date");
        }

        if (createRequest.getStartDate().isBefore(LocalDateTime.now().minusMinutes(5))) {
            throw new IllegalArgumentException("Start date cannot be in the past");
        }

        // Validate attachments if present
        if (createRequest.getAttachments() != null && !createRequest.getAttachments().isEmpty()) {
            pollAttachmentService.validatePollAttachments(createRequest.getAttachments());
        }
    }

    @Override
    public void validateUpdatePollRequest(Poll poll, UpdatePollRequest updateRequest, HttpServletRequest request) {
        if (poll == null) {
            throw new IllegalStateException("Poll does not exist");
        }

        if (updateRequest == null) {
            throw new IllegalStateException("Update poll does not exist");
        }

        // Check if poll can be updated
        if (poll.getStatus() == PollStatus.CLOSED || poll.getStatus() == PollStatus.ARCHIVED) {
            throw new IllegalArgumentException("Poll status cannot be CLOSED or ARCHIVED");
        }

        // validate date updates
        validateDateUpdates(poll, updateRequest);

        //validate voting settings changes
        validateVotingSettingsChanges(poll, updateRequest);
    }

    @Override
    public boolean hasPermissionToModifyPoll(Poll poll, HttpServletRequest request) {
        try {
            Long currentUserId = jwtHelperService.getCurrentUserIdFromRequest(request);
            Role currentUserRole = jwtHelperService.getCurrentUserRoleFromRequest(request);

            if (currentUserRole == Role.ADMIN) {
                return true;
            }

            return poll.getCreator().getId().equals(currentUserId);
        } catch (Exception e) {
            log.error(e.getMessage());
            return false;
        }
    }

    private User getAuthenticatedUser(HttpServletRequest request) {
        Long userId = jwtHelperService.getCurrentUserIdFromRequest(request);
        return userRepository.findById(userId)
                .orElseThrow(() -> new CustomException("User not found"));
    }

    private Poll getPollById(Long pollId) {
        return pollRepository.findById(pollId)
                .orElseThrow(() -> new CustomException("Poll does not exist"));
    }


    private void validateUserPermissions(Poll poll, HttpServletRequest request) {
        if (!hasPermissionToModifyPoll(poll, request)) {
            throw new CustomException("You don't have permission to modify this poll");
        }
    }


    private void validateAdminPermissions(HttpServletRequest request) {
        if (!jwtHelperService.isAdmin(request)) {
            throw new CustomException("Admin privileges required for this operation");
        }
    }

    private void validatePollForActivation(Poll poll) {
        if (poll.getStatus() != PollStatus.DRAFT) {
            throw new IllegalStateException("Only draft polls can be activated");
        }

        if (poll.getStartDate().isBefore(LocalDateTime.now())) {
            throw new IllegalStateException("Cannot activate poll with past start date");
        }
    }

    private void validatePollForClosure(Poll poll) {
        if (poll.getStatus() != PollStatus.ACTIVE) {
            throw new IllegalStateException("Only active polls can be closed");
        }
    }

    private void validatePollForDeletion(Poll poll) {
        if (poll.getTotalVotes() > 0) {
            throw new IllegalStateException("Cannot delete poll that has received votes");
        }
    }

    private void validateDateUpdates(Poll poll, UpdatePollRequest updateRequest) {
        if (updateRequest.getStartDate() != null && updateRequest.getEndDate() != null) {
            if (updateRequest.getEndDate().isBefore(updateRequest.getStartDate())) {
                throw new IllegalArgumentException("End date must be after start date");
            }
        } else if (updateRequest.getEndDate() != null) {
            if (updateRequest.getEndDate().isBefore(poll.getStartDate())) {
                throw new IllegalArgumentException("End date must be after current start date");
            }
        } else if (updateRequest.getStartDate() != null) {
            if (poll.getEndDate().isBefore(updateRequest.getStartDate())) {
                throw new IllegalArgumentException("Start date must be before current end date");
            }
        }
    }

    private void validateVotingSettingsChanges(Poll poll, UpdatePollRequest updatePollRequest) {
        // if poll is active and has votes, restrict certain updates
        if (poll.getStatus() == PollStatus.ACTIVE && poll.getTotalVotes() > 0) {
            if (updatePollRequest.getAllowAnonymousVoting() != null && !updatePollRequest.getAllowAnonymousVoting().equals(poll.getAllowAnonymousVoting())) {
                throw new IllegalArgumentException("Can't update anonymous voting settings");
            }
        }
    }
}
