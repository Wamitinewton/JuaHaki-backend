package com.juahaki.juahaki.core.poll.service.opinion;

import com.juahaki.juahaki.core.poll.dto.filters.OpinionFilterRequest;
import com.juahaki.juahaki.core.poll.dto.opinions.OpinionReactionResponse;
import com.juahaki.juahaki.core.poll.dto.opinions.OpinionResponse;
import com.juahaki.juahaki.core.poll.dto.opinions.ReactToOpinionRequest;
import com.juahaki.juahaki.core.poll.dto.opinions.SubmitOpinionRequest;
import com.juahaki.juahaki.core.poll.model.Poll;
import com.juahaki.juahaki.core.poll.model.PollOpinion;
import com.juahaki.juahaki.shared.exception.CustomException;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

/**
 * Service interface for managing poll opinions and reactions.
 * Handles opinion submission, reactions (likes/dislikes), and opinion retrieval with filtering.
 * Supports both anonymous and authenticated opinion sharing with attachment capabilities.
 */
public interface IPollOpinionService {

    /**
     * Submits a new opinion on a poll with optional attachments (images, documents).
     * Users can provide context, evidence, or detailed reasoning for their stance.
     * Supports both anonymous and authenticated opinion submission.
     *
     * @param submitOpinionRequest the opinion submission request containing content, stance, and attachments
     * @param request              the HTTP request to extract user authentication and identification details
     * @return OpinionResponse containing the submitted opinion with processed attachments
     * @throws CustomException          if the poll doesn't allow opinions or user lacks permission
     * @throws IllegalArgumentException if the opinion request is invalid
     * @throws IllegalStateException    if the poll is not in a state that accepts opinions
     */
    OpinionResponse submitOpinion(SubmitOpinionRequest submitOpinionRequest, HttpServletRequest request);

    /**
     * Updates an existing opinion content and attachments.
     * Only the opinion author or administrators can modify opinions.
     * Edit history may be maintained for transparency.
     *
     * @param opinionId            the unique identifier of the opinion to update
     * @param submitOpinionRequest the updated opinion content and attachments
     * @param request              the HTTP request to extract user authentication details
     * @return OpinionResponse containing the updated opinion information
     * @throws CustomException       if the opinion is not found or user lacks permission
     * @throws IllegalStateException if the opinion cannot be edited (e.g., too old, poll closed)
     */
    OpinionResponse updateOpinion(Long opinionId, SubmitOpinionRequest submitOpinionRequest, HttpServletRequest request);

    /**
     * Deletes an opinion and all associated reactions and attachments.
     * Only the opinion author or administrators can delete opinions.
     *
     * @param opinionId the unique identifier of the opinion to delete
     * @param request   the HTTP request to extract user authentication details
     * @throws CustomException if the opinion is not found or user lacks permission
     */
    void deleteOpinion(Long opinionId, HttpServletRequest request);

    /**
     * Reacts to an opinion with like or dislike.
     * Users can change their reaction or remove it entirely.
     * Anonymous reactions are supported based on poll configuration.
     *
     * @param reactRequest the reaction request containing opinion ID and reaction type
     * @param request      the HTTP request to extract user identification details
     * @return OpinionReactionResponse containing reaction confirmation and updated counts
     * @throws CustomException          if the opinion is not found or reactions are not allowed
     * @throws IllegalArgumentException if the reaction request is invalid
     */
    OpinionReactionResponse reactToOpinion(ReactToOpinionRequest reactRequest, HttpServletRequest request);

    /**
     * Removes a user's reaction from an opinion.
     *
     * @param opinionId the unique identifier of the opinion
     * @param request   the HTTP request to extract user identification details
     * @return OpinionReactionResponse containing updated reaction counts
     * @throws CustomException if the opinion is not found or user hasn't reacted
     */
    OpinionReactionResponse removeReaction(Long opinionId, HttpServletRequest request);

    /**
     * Retrieves paginated opinions for a poll with optional filtering and sorting.
     * Supports filtering by stance, author type, date range, and minimum likes.
     *
     * @param filterRequest the filter criteria for opinion retrieval
     * @param pageable      pagination and sorting parameters
     * @param request       the HTTP request to extract user context for personalization
     * @return Page of OpinionResponse objects matching the filter criteria
     */
    Page<OpinionResponse> getFilteredOpinions(OpinionFilterRequest filterRequest, Pageable pageable, HttpServletRequest request);

    /**
     * Retrieves the most liked opinions for a specific poll.
     * Used for displaying featured or top opinions in the UI.
     *
     * @param pollId  the unique identifier of the poll
     * @param limit   the maximum number of top opinions to retrieve
     * @param request the HTTP request to extract user context
     * @return List of top-rated OpinionResponse objects
     */
    List<OpinionResponse> getTopOpinions(Long pollId, int limit, HttpServletRequest request);

    /**
     * Retrieves the most recent opinions for a specific poll.
     * Provides real-time opinion feed for active discussions.
     *
     * @param pollId  the unique identifier of the poll
     * @param limit   the maximum number of recent opinions to retrieve
     * @param request the HTTP request to extract user context
     * @return List of recent OpinionResponse objects
     */
    List<OpinionResponse> getRecentOpinions(Long pollId, int limit, HttpServletRequest request);

    /**
     * Gets all opinions submitted by a specific user across all polls.
     * Used for user profile and activity tracking.
     *
     * @param pageable pagination parameters
     * @param request  the HTTP request to extract user authentication details
     * @return Page of OpinionResponse objects submitted by the authenticated user
     */
    Page<OpinionResponse> getUserOpinions(Pageable pageable, HttpServletRequest request);

    /**
     * Validates opinion submission request including content moderation checks.
     * Ensures opinions meet community guidelines and platform policies.
     *
     * @param poll                 the poll entity the opinion is being submitted to
     * @param submitOpinionRequest the opinion request to validate
     * @param request              the HTTP request for additional validation context
     * @throws IllegalArgumentException if the opinion content is invalid
     * @throws CustomException          if opinion submission violates platform policies
     */
    void validateOpinionSubmission(Poll poll, SubmitOpinionRequest submitOpinionRequest, HttpServletRequest request);

    /**
     * Validates reaction request ensuring the user can react to the specified opinion.
     * Checks opinion existence, reaction permissions, and previous reaction status.
     *
     * @param opinion      the opinion entity being reacted to
     * @param reactRequest the reaction request to validate
     * @param request      the HTTP request for validation context
     * @throws IllegalArgumentException if the reaction request is invalid
     * @throws CustomException          if the user cannot react to this opinion
     */
    void validateReactionRequest(PollOpinion opinion, ReactToOpinionRequest reactRequest, HttpServletRequest request);

    /**
     * Checks if a user has permission to modify (edit/delete) a specific opinion.
     * Considers opinion ownership, user roles, and platform policies.
     *
     * @param opinion the opinion entity to check permissions for
     * @param request the HTTP request to extract user authentication details
     * @return true if the user can modify the opinion, false otherwise
     */
    boolean canModifyOpinion(PollOpinion opinion, HttpServletRequest request);

    /**
     * Checks if opinions are currently allowed for a specific poll.
     * Considers poll status, configuration, and timing constraints.
     *
     * @param poll the poll entity to check
     * @return true if opinions are allowed, false otherwise
     */
    boolean areOpinionsAllowed(Poll poll);

    /**
     * Updates opinion statistics including like/dislike counts after a reaction.
     * Ensures atomicity and consistency of reaction tallies.
     *
     * @param opinion              the opinion entity to update
     * @param previousReactionType the previous reaction type if changing reaction (null for new reactions)
     * @param newReactionType      the new reaction type being applied
     */
    void updateOpinionReactionStatistics(PollOpinion opinion, String previousReactionType, String newReactionType);
}
