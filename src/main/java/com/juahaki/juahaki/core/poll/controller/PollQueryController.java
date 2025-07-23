package com.juahaki.juahaki.core.poll.controller;

import com.juahaki.juahaki.core.poll.dto.filters.PollFilterRequest;
import com.juahaki.juahaki.core.poll.dto.filters.PollStatsRequest;
import com.juahaki.juahaki.core.poll.dto.results.PollDetailsResponse;
import com.juahaki.juahaki.core.poll.dto.results.PollListResponse;
import com.juahaki.juahaki.core.poll.dto.results.PollSummaryResponse;
import com.juahaki.juahaki.core.poll.service.query.IPollQueryService;
import com.juahaki.juahaki.shared.dto.response.ApiResponse;
import com.juahaki.juahaki.shared.enums.PollCategory;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("${api.prefix}/polls")
@RequiredArgsConstructor
@Slf4j
public class PollQueryController {

    private final IPollQueryService pollQueryService;

    @GetMapping("/{pollId}")
    public ResponseEntity<ApiResponse> getPollDetails(
            @PathVariable Long pollId,
            HttpServletRequest request) {

        log.debug("Getting poll details for ID: {}", pollId);

        PollDetailsResponse response = pollQueryService.getPollDetails(pollId, request);

        return ResponseEntity.ok(new ApiResponse("Poll details retrieved successfully", response));
    }

    @GetMapping("/{pollId}/summary")
    public ResponseEntity<ApiResponse> getPollSummary(
            @PathVariable Long pollId,
            HttpServletRequest request) {

        log.debug("Getting poll summary for ID: {}", pollId);

        PollSummaryResponse response = pollQueryService.getPollSummary(pollId, request);

        return ResponseEntity.ok(new ApiResponse("Poll summary retrieved successfully", response));
    }

    @GetMapping
    public ResponseEntity<ApiResponse> getFilteredPolls(
            @ModelAttribute PollFilterRequest filterRequest,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable,
            HttpServletRequest request) {

        log.debug("Getting filtered polls with filter: {}", filterRequest);

        Page<PollListResponse> response = pollQueryService.getFilteredPolls(filterRequest, pageable, request);

        return ResponseEntity.ok(new ApiResponse("Polls retrieved successfully", response));
    }

    @GetMapping("/active")
    public ResponseEntity<ApiResponse> getActivePolls(
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable,
            HttpServletRequest request) {

        log.debug("Getting active polls");

        Page<PollListResponse> response = pollQueryService.getActivePolls(pageable, request);

        return ResponseEntity.ok(new ApiResponse("Active polls retrieved successfully", response));
    }

    @GetMapping("/my-polls")
    public ResponseEntity<ApiResponse> getUserCreatedPolls(
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable,
            HttpServletRequest request) {

        log.debug("Getting user created polls");

        Page<PollListResponse> response = pollQueryService.getUserCreatedPolls(pageable, request);

        return ResponseEntity.ok(new ApiResponse("User polls retrieved successfully", response));
    }

    @GetMapping("/my-votes")
    public ResponseEntity<ApiResponse> getUserVotedPolls(
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable,
            HttpServletRequest request) {

        log.debug("Getting user voted polls");

        Page<PollListResponse> response = pollQueryService.getUserVotedPolls(pageable, request);

        return ResponseEntity.ok(new ApiResponse("User voted polls retrieved successfully", response));
    }

    @GetMapping("/category/{category}")
    public ResponseEntity<ApiResponse> getPollsByCategory(
            @PathVariable PollCategory category,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable,
            HttpServletRequest request) {

        log.debug("Getting polls by category: {}", category);

        Page<PollListResponse> response = pollQueryService.getPollsByCategory(category, pageable, request);

        return ResponseEntity.ok(new ApiResponse("Polls by category retrieved successfully", response));
    }

    @GetMapping("/search")
    public ResponseEntity<ApiResponse> searchPolls(
            @RequestParam String query,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable,
            HttpServletRequest request) {

        log.debug("Searching polls with query: {}", query);

        Page<PollListResponse> response = pollQueryService.searchPolls(query, pageable, request);

        return ResponseEntity.ok(new ApiResponse("Poll search results retrieved successfully", response));
    }

    @GetMapping("/trending")
    public ResponseEntity<ApiResponse> getTrendingPolls(
            @RequestParam(defaultValue = "10") int limit,
            HttpServletRequest request) {

        log.debug("Getting trending polls with limit: {}", limit);

        List<PollListResponse> response = pollQueryService.getTrendingPolls(limit, request);

        return ResponseEntity.ok(new ApiResponse("Trending polls retrieved successfully", response));
    }

    @GetMapping("/recent")
    public ResponseEntity<ApiResponse> getRecentPolls(
            @RequestParam(defaultValue = "10") int limit,
            HttpServletRequest request) {

        log.debug("Getting recent polls with limit: {}", limit);

        List<PollListResponse> response = pollQueryService.getRecentPolls(limit, request);

        return ResponseEntity.ok(new ApiResponse("Recent polls retrieved successfully", response));
    }

    @GetMapping("/expiring")
    public ResponseEntity<ApiResponse> getExpiringPolls(
            @RequestParam(defaultValue = "24") int hoursUntilExpiry,
            @RequestParam(defaultValue = "10") int limit,
            HttpServletRequest request) {

        log.debug("Getting expiring polls: {} hours, limit: {}", hoursUntilExpiry, limit);

        List<PollListResponse> response = pollQueryService.getExpiringPolls(hoursUntilExpiry, limit, request);

        return ResponseEntity.ok(new ApiResponse("Expiring polls retrieved successfully", response));
    }

    @GetMapping("/archived")
    public ResponseEntity<ApiResponse> getArchivedPolls(
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable,
            HttpServletRequest request) {

        log.debug("Getting archived polls");

        Page<PollListResponse> response = pollQueryService.getArchivedPolls(pageable, request);

        return ResponseEntity.ok(new ApiResponse("Archived polls retrieved successfully", response));
    }

    @GetMapping("/statistics")
    public ResponseEntity<ApiResponse> getPollStatistics(
            @ModelAttribute PollStatsRequest statsRequest,
            HttpServletRequest request) {

        log.debug("Getting poll statistics with request: {}", statsRequest);

        Map<String, Object> response = pollQueryService.getPollStatistics(statsRequest, request);

        return ResponseEntity.ok(new ApiResponse("Poll statistics retrieved successfully", response));
    }

    @GetMapping("/{pollId}/participation-stats")
    public ResponseEntity<ApiResponse> getPollParticipationStats(
            @PathVariable Long pollId,
            HttpServletRequest request) {

        log.debug("Getting participation stats for poll ID: {}", pollId);

        Map<String, Object> response = pollQueryService.getPollParticipationStats(pollId, request);

        return ResponseEntity.ok(new ApiResponse("Poll participation statistics retrieved successfully", response));
    }
}