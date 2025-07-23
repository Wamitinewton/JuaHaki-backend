package com.juahaki.juahaki.core.poll.service.query;

import com.juahaki.juahaki.core.poll.dto.filters.PollFilterRequest;
import com.juahaki.juahaki.core.poll.dto.filters.PollStatsRequest;
import com.juahaki.juahaki.core.poll.dto.results.PollDetailsResponse;
import com.juahaki.juahaki.core.poll.dto.results.PollListResponse;
import com.juahaki.juahaki.core.poll.dto.results.PollSummaryResponse;
import com.juahaki.juahaki.shared.enums.PollCategory;
import com.juahaki.juahaki.shared.exception.CustomException;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Map;

/**
 * Service interface for querying and retrieving poll information.
 * Handles read operations, filtering, searching, and statistical analysis of polls.
 * Provides various views and aggregations for different user interfaces and reporting needs.
 */
public interface IPollQueryService {

    /**
     * Retrieves detailed information about a specific poll including voting results and top opinions.
     * Provides comprehensive view suitable for poll detail pages.
     *
     * @param pollId  the unique identifier of the poll
     * @param request the HTTP request to extract user context for personalization
     * @return PollDetailsResponse containing poll information, results, and featured opinions
     * @throws CustomException if the poll is not found or user lacks access permission
     */
    PollDetailsResponse getPollDetails(Long pollId, HttpServletRequest request);

    /**
     * Retrieves summary information about a specific poll.
     * Provides essential poll data without detailed opinions for list views.
     *
     * @param pollId  the unique identifier of the poll
     * @param request the HTTP request to extract user context
     * @return PollSummaryResponse containing poll summary and voting statistics
     * @throws CustomException if the poll is not found
     */
    PollSummaryResponse getPollSummary(Long pollId, HttpServletRequest request);

    /**
     * Retrieves paginated list of polls with filtering and sorting capabilities.
     * Supports filtering by category, status, creator, date range, and search terms.
     *
     * @param filterRequest the filter criteria including category, status, search terms, etc.
     * @param pageable      pagination and sorting parameters
     * @param request       the HTTP request to extract user context
     * @return Page of PollListResponse objects matching the filter criteria
     */
    Page<PollListResponse> getFilteredPolls(PollFilterRequest filterRequest, Pageable pageable, HttpServletRequest request);

    /**
     * Retrieves all active polls that are currently accepting votes.
     * Used for displaying current voting opportunities to users.
     *
     * @param pageable pagination parameters
     * @param request  the HTTP request to extract user context
     * @return Page of active PollListResponse objects
     */
    Page<PollListResponse> getActivePolls(Pageable pageable, HttpServletRequest request);

    /**
     * Retrieves polls created by a specific user.
     * Used for user profile pages and poll management interfaces.
     *
     * @param pageable pagination parameters
     * @param request  the HTTP request to extract user authentication details
     * @return Page of PollListResponse objects created by the authenticated user
     */
    Page<PollListResponse> getUserCreatedPolls(Pageable pageable, HttpServletRequest request);

    /**
     * Retrieves polls that a user has voted on.
     * Helps users track their voting participation history.
     *
     * @param pageable pagination parameters
     * @param request  the HTTP request to extract user authentication details
     * @return Page of PollListResponse objects the user has participated in
     */
    Page<PollListResponse> getUserVotedPolls(Pageable pageable, HttpServletRequest request);

    /**
     * Retrieves polls by category with optional sub-filtering.
     * Used for category-specific poll browsing and navigation.
     *
     * @param category the poll category to filter by
     * @param pageable pagination and sorting parameters
     * @param request  the HTTP request to extract user context
     * @return Page of PollListResponse objects in the specified category
     */
    Page<PollListResponse> getPollsByCategory(PollCategory category, Pageable pageable, HttpServletRequest request);

    /**
     * Searches polls by text content including title and description.
     * Provides full-text search capabilities across poll content.
     *
     * @param searchTerm the text to search for in poll titles and descriptions
     * @param pageable   pagination and sorting parameters
     * @param request    the HTTP request to extract user context
     * @return Page of PollListResponse objects matching the search criteria
     */
    Page<PollListResponse> searchPolls(String searchTerm, Pageable pageable, HttpServletRequest request);

    /**
     * Retrieves trending or popular polls based on recent activity and engagement.
     * Used for highlighting high-engagement polls on discovery pages.
     *
     * @param limit   the maximum number of trending polls to retrieve
     * @param request the HTTP request to extract user context
     * @return List of trending PollListResponse objects
     */
    List<PollListResponse> getTrendingPolls(int limit, HttpServletRequest request);

    /**
     * Retrieves recently created polls for discovery and timeline features.
     *
     * @param limit   the maximum number of recent polls to retrieve
     * @param request the HTTP request to extract user context
     * @return List of recent PollListResponse objects
     */
    List<PollListResponse> getRecentPolls(int limit, HttpServletRequest request);

    /**
     * Retrieves polls that are expiring soon to encourage last-minute participation.
     *
     * @param hoursUntilExpiry the number of hours to look ahead for expiring polls
     * @param limit            the maximum number of expiring polls to retrieve
     * @param request          the HTTP request to extract user context
     * @return List of soon-to-expire PollListResponse objects
     */
    List<PollListResponse> getExpiringPolls(int hoursUntilExpiry, int limit, HttpServletRequest request);

    /**
     * Generates comprehensive statistics for polls based on filter criteria.
     * Used for analytics dashboards and reporting interfaces.
     *
     * @param statsRequest the criteria for statistical analysis
     * @param request      the HTTP request to extract user context
     * @return Map containing various statistical metrics and aggregations
     */
    Map<String, Object> getPollStatistics(PollStatsRequest statsRequest, HttpServletRequest request);

    /**
     * Retrieves poll participation statistics including vote counts and engagement metrics.
     *
     * @param pollId  the unique identifier of the poll
     * @param request the HTTP request to extract user context
     * @return Map containing detailed participation statistics
     */
    Map<String, Object> getPollParticipationStats(Long pollId, HttpServletRequest request);


    /**
     * Checks if a user has access to view a specific poll.
     * Considers poll visibility settings, user permissions, and poll status.
     *
     * @param pollId  the unique identifier of the poll
     * @param request the HTTP request to extract user authentication details
     * @return true if the user can view the poll, false otherwise
     */
    boolean canUserViewPoll(Long pollId, HttpServletRequest request);


    /**
     * Gets archived polls for historical analysis and reference.
     *
     * @param pageable pagination parameters
     * @param request  the HTTP request to extract user context
     * @return Page of archived PollListResponse objects
     */
    Page<PollListResponse> getArchivedPolls(Pageable pageable, HttpServletRequest request);
}
