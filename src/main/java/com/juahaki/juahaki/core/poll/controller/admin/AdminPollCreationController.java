package com.juahaki.juahaki.core.poll.controller.admin;

import com.juahaki.juahaki.core.poll.service.creation.IPollCreationService;
import com.juahaki.juahaki.shared.dto.response.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("${api.prefix}/admin/polls")
@RequiredArgsConstructor
@Slf4j
public class AdminPollCreationController {

    private final IPollCreationService pollCreationService;

    @PostMapping("/{pollId}/suspend")
    public ResponseEntity<ApiResponse> suspendPoll(
            @PathVariable Long pollId,
            HttpServletRequest request) {

        log.info("Admin suspending poll with ID: {}", pollId);

        pollCreationService.suspendPoll(pollId, request);

        return ResponseEntity.ok(new ApiResponse("Poll suspended successfully", null));
    }

    @DeleteMapping("/{pollId}")
    public ResponseEntity<ApiResponse> deletePoll(
            @PathVariable Long pollId,
            HttpServletRequest request) {

        log.info("Admin deleting poll with ID: {}", pollId);

        pollCreationService.deletePoll(pollId, request);

        return ResponseEntity.ok(new ApiResponse("Poll deleted successfully", null));
    }

    @PostMapping("/{pollId}/force-close")
    public ResponseEntity<ApiResponse> forceClosePoll(
            @PathVariable Long pollId,
            HttpServletRequest request) {

        log.info("Admin force closing poll with ID: {}", pollId);

        pollCreationService.closePoll(pollId, request);

        return ResponseEntity.ok(new ApiResponse("Poll force closed successfully", null));
    }

    @PostMapping("/{pollId}/force-archive")
    public ResponseEntity<ApiResponse> forceArchivePoll(
            @PathVariable Long pollId,
            HttpServletRequest request) {

        log.info("Admin force archiving poll with ID: {}", pollId);

        pollCreationService.archivePoll(pollId, request);

        return ResponseEntity.ok(new ApiResponse("Poll force archived successfully", null));
    }
}
