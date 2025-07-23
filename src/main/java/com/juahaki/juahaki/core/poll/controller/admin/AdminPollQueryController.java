package com.juahaki.juahaki.core.poll.controller.admin;

import com.juahaki.juahaki.core.poll.dto.filters.PollFilterRequest;
import com.juahaki.juahaki.core.poll.dto.filters.PollStatsRequest;
import com.juahaki.juahaki.core.poll.dto.results.PollListResponse;
import com.juahaki.juahaki.core.poll.service.query.IPollQueryService;
import com.juahaki.juahaki.shared.dto.response.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("${api.prefix}/admin/polls")
@RequiredArgsConstructor
@Slf4j
public class AdminPollQueryController {

    private final IPollQueryService pollQueryService;

    @GetMapping("/all")
    public ResponseEntity<ApiResponse> getAllPolls(
            @ModelAttribute PollFilterRequest filterRequest,
            @PageableDefault(size = 50, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable,
            HttpServletRequest request) {

        log.debug("Admin getting all polls with filter: {}", filterRequest);

        Page<PollListResponse> response = pollQueryService.getFilteredPolls(filterRequest, pageable, request);

        return ResponseEntity.ok(new ApiResponse("All polls retrieved successfully", response));
    }

    @GetMapping("/suspended")
    public ResponseEntity<ApiResponse> getSuspendedPolls(
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable,
            HttpServletRequest request) {

        log.debug("Admin getting suspended polls");

        PollFilterRequest filterRequest = PollFilterRequest.builder()
                .status(com.juahaki.juahaki.shared.enums.PollStatus.SUSPENDED)
                .build();

        Page<PollListResponse> response = pollQueryService.getFilteredPolls(filterRequest, pageable, request);

        return ResponseEntity.ok(new ApiResponse("Suspended polls retrieved successfully", response));
    }

    @GetMapping("/analytics/overview")
    public ResponseEntity<ApiResponse> getPollAnalyticsOverview(
            @ModelAttribute PollStatsRequest statsRequest,
            HttpServletRequest request) {

        log.debug("Admin getting poll analytics overview");

        Map<String, Object> response = pollQueryService.getPollStatistics(statsRequest, request);

        return ResponseEntity.ok(new ApiResponse("Poll analytics overview retrieved successfully", response));
    }

    @GetMapping("/{pollId}/admin-details")
    public ResponseEntity<ApiResponse> getPollAdminDetails(
            @PathVariable Long pollId,
            HttpServletRequest request) {

        log.debug("Admin getting detailed view for poll ID: {}", pollId);

        var pollDetails = pollQueryService.getPollDetails(pollId, request);
        var participationStats = pollQueryService.getPollParticipationStats(pollId, request);

        Map<String, Object> adminDetails = Map.of(
                "poll", pollDetails,
                "participationStats", participationStats
        );

        return ResponseEntity.ok(new ApiResponse("Poll admin details retrieved successfully", adminDetails));
    }

    @GetMapping("/reports/summary")
    public ResponseEntity<ApiResponse> getPollsSummaryReport(
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            HttpServletRequest request) {

        log.debug("Admin generating polls summary report from {} to {}", startDate, endDate);

        PollStatsRequest statsRequest = PollStatsRequest.builder()
                .fromDate(startDate != null ? java.time.LocalDateTime.parse(startDate) : null)
                .toDate(endDate != null ? java.time.LocalDateTime.parse(endDate) : null)
                .build();

        Map<String, Object> report = pollQueryService.getPollStatistics(statsRequest, request);

        return ResponseEntity.ok(new ApiResponse("Polls summary report generated successfully", report));
    }
}
