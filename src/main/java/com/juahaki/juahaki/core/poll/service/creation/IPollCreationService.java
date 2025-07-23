package com.juahaki.juahaki.core.poll.service.creation;


import com.juahaki.juahaki.core.poll.dto.creation.CreatePollRequest;
import com.juahaki.juahaki.core.poll.dto.creation.CreatePollResponse;
import com.juahaki.juahaki.core.poll.dto.creation.UpdatePollRequest;
import com.juahaki.juahaki.core.poll.model.Poll;
import com.juahaki.juahaki.shared.exception.CustomException;
import jakarta.servlet.http.HttpServletRequest;

/**
 * Service interface for managing poll creation and basic poll operations.
 * Handles the lifecycle of polls including creation, updates, and deletion.
 */
public interface IPollCreationService {

    /**
     * Creates a new poll with the provided details and optional attachments.
     * The creator must be authenticated and the poll will be associated with their account.
     *
     * @param createRequest the poll creation request containing title, description, category, etc.
     * @param request the HTTP request to extract user authentication details
     * @return CreatePollResponse containing the created poll information and attachment details
     * @throws IllegalArgumentException if the request is invalid or missing required fields
     * @throws CustomException if poll creation fails due to business logic constraints
     */
    CreatePollResponse createPoll(CreatePollRequest createRequest, HttpServletRequest request);

    /**
     * Updates an existing poll with new information.
     * Only the poll creator or administrators can update a poll.
     * Active polls may have limited update capabilities to maintain voting integrity.
     *
     * @param pollId the unique identifier of the poll to update
     * @param updateRequest the update request containing modified poll details
     * @param request the HTTP request to extract user authentication details
     * @return CreatePollResponse containing the updated poll information
     * @throws CustomException if the poll is not found or user lacks permission
     * @throws IllegalStateException if the poll status doesn't allow updates
     */
    CreatePollResponse updatePoll(Long pollId, UpdatePollRequest updateRequest, HttpServletRequest request);

    /**
     * Activates a draft poll, making it available for public voting.
     * Only poll creators or administrators can activate polls.
     *
     * @param pollId the unique identifier of the poll to activate
     * @param request the HTTP request to extract user authentication details
     * @return CreatePollResponse containing the activated poll information
     * @throws CustomException if the poll is not found or cannot be activated
     * @throws IllegalStateException if the poll is not in DRAFT status
     */
    CreatePollResponse activatePoll(Long pollId, HttpServletRequest request);

    /**
     * Closes an active poll, preventing further votes and opinions.
     * Only poll creators or administrators can close polls.
     *
     * @param pollId the unique identifier of the poll to close
     * @param request the HTTP request to extract user authentication details
     * @return CreatePollResponse containing the closed poll information
     * @throws CustomException if the poll is not found or user lacks permission
     */
    CreatePollResponse closePoll(Long pollId, HttpServletRequest request);

    /**
     * Suspends a poll temporarily, preventing votes while keeping it visible.
     * This is typically used for moderation purposes by administrators.
     *
     * @param pollId the unique identifier of the poll to suspend
     * @param request the HTTP request to extract user authentication details
     * @throws CustomException if the poll is not found or user lacks permission
     */
    void suspendPoll(Long pollId, HttpServletRequest request);

    /**
     * Permanently deletes a poll and all associated data including votes and opinions.
     * This action cannot be undone. Only administrators can delete polls.
     *
     * @param pollId the unique identifier of the poll to delete
     * @param request the HTTP request to extract user authentication details
     * @throws CustomException if the poll is not found or user lacks permission
     * @throws IllegalStateException if the poll has votes and cannot be deleted
     */
    void deletePoll(Long pollId, HttpServletRequest request);

    /**
     * Archives old polls to reduce database size while maintaining historical records.
     * This is typically called by administrators or automated cleanup processes.
     *
     * @param pollId the unique identifier of the poll to archive
     * @param request the HTTP request to extract user authentication details
     * @throws CustomException if the poll is not found or user lacks permission
     */
    void archivePoll(Long pollId, HttpServletRequest request);

    /**
     * Validates poll creation request data including business rules.
     * This method performs comprehensive validation beyond basic field validation.
     *
     * @param createRequest the poll creation request to validate
     * @param request the HTTP request to extract user context
     * @throws IllegalArgumentException if validation fails with detailed error messages
     */
    void validateCreatePollRequest(CreatePollRequest createRequest, HttpServletRequest request);

    /**
     * Validates poll update request ensuring only allowed modifications are made.
     * Different validation rules apply based on poll status and user permissions.
     *
     * @param poll the existing poll entity
     * @param updateRequest the update request to validate
     * @param request the HTTP request to extract user context
     * @throws IllegalArgumentException if validation fails
     * @throws IllegalStateException if the poll state doesn't allow updates
     */
    void validateUpdatePollRequest(Poll poll, UpdatePollRequest updateRequest, HttpServletRequest request);

    /**
     * Checks if a user has permission to perform actions on a specific poll.
     * Considers user roles, poll ownership, and poll status.
     *
     * @param poll the poll entity to check permissions for
     * @param request the HTTP request to extract user authentication details
     * @return true if the user has permission, false otherwise
     */
    boolean hasPermissionToModifyPoll(Poll poll, HttpServletRequest request);
}