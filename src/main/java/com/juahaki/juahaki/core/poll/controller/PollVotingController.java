package com.juahaki.juahaki.core.poll.controller;

import com.juahaki.juahaki.core.poll.dto.voting.SubmitVoteRequest;
import com.juahaki.juahaki.core.poll.dto.voting.SubmitVoteResponse;
import com.juahaki.juahaki.core.poll.dto.voting.VoteStatusResponse;
import com.juahaki.juahaki.core.poll.service.voting.IPollVotingService;
import com.juahaki.juahaki.shared.dto.response.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("${api.prefix}/polls")
@RequiredArgsConstructor
@Slf4j
public class PollVotingController {

    private final IPollVotingService pollVotingService;

    @PostMapping("/vote")
    public ResponseEntity<ApiResponse> submitVote(
            @Valid @RequestBody SubmitVoteRequest submitVoteRequest,
            HttpServletRequest request) {

        log.info("Submitting vote for poll ID: {}", submitVoteRequest.getPollId());

        SubmitVoteResponse response = pollVotingService.submitVote(submitVoteRequest, request);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new ApiResponse("Vote submitted successfully", response));
    }

    @PutMapping("/vote")
    public ResponseEntity<ApiResponse> changeVote(
            @Valid @RequestBody SubmitVoteRequest submitVoteRequest,
            HttpServletRequest request) {

        log.info("Changing vote for poll ID: {}", submitVoteRequest.getPollId());

        SubmitVoteResponse response = pollVotingService.changeVote(submitVoteRequest, request);

        return ResponseEntity.ok(new ApiResponse("Vote changed successfully", response));
    }

    @GetMapping("/{pollId}/vote-status")
    public ResponseEntity<ApiResponse> getUserVoteStatus(
            @PathVariable Long pollId,
            HttpServletRequest request) {

        log.debug("Getting vote status for poll ID: {}", pollId);

        VoteStatusResponse response = pollVotingService.getUserVoteStatus(pollId, request);

        return ResponseEntity.ok(new ApiResponse("Vote status retrieved successfully", response));
    }

    @DeleteMapping("/{pollId}/vote")
    public ResponseEntity<ApiResponse> withdrawVote(
            @PathVariable Long pollId,
            HttpServletRequest request) {

        log.info("Withdrawing vote for poll ID: {}", pollId);

        pollVotingService.withdrawVote(pollId, request);

        return ResponseEntity.ok(new ApiResponse("Vote withdrawn successfully", null));
    }
}
