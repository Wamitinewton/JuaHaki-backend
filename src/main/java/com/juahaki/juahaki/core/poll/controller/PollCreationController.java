package com.juahaki.juahaki.core.poll.controller;

import com.juahaki.juahaki.core.poll.dto.creation.CreatePollRequest;
import com.juahaki.juahaki.core.poll.dto.creation.CreatePollResponse;
import com.juahaki.juahaki.core.poll.dto.creation.UpdatePollRequest;
import com.juahaki.juahaki.core.poll.service.creation.IPollCreationService;
import com.juahaki.juahaki.shared.dto.response.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("${api.prefix}/polls")
@RequiredArgsConstructor
@Slf4j
public class PollCreationController {

    private final IPollCreationService pollCreationService;

    @PostMapping(value = "/create" ,consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse> createPoll(
            @Valid @ModelAttribute CreatePollRequest createPollRequest,
            HttpServletRequest request) {

        log.info("Creating new poll: {}", createPollRequest.getTitle());

        CreatePollResponse response = pollCreationService.createPoll(createPollRequest, request);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new ApiResponse("Poll created successfully", response));
    }

    @PutMapping("/update/{pollId}")
    public ResponseEntity<ApiResponse> updatePoll(
            @PathVariable Long pollId,
            @Valid @RequestBody UpdatePollRequest updatePollRequest,
            HttpServletRequest request) {

        log.info("Updating poll with ID: {}", pollId);

        CreatePollResponse response = pollCreationService.updatePoll(pollId, updatePollRequest, request);

        return ResponseEntity.ok(new ApiResponse("Poll updated successfully", response));
    }

    @PostMapping("/{pollId}/activate")
    public ResponseEntity<ApiResponse> activatePoll(
            @PathVariable Long pollId,
            HttpServletRequest request) {

        log.info("Activating poll with ID: {}", pollId);

        CreatePollResponse response = pollCreationService.activatePoll(pollId, request);

        return ResponseEntity.ok(new ApiResponse("Poll activated successfully", response));
    }

    @PostMapping("/{pollId}/close")
    public ResponseEntity<ApiResponse> closePoll(
            @PathVariable Long pollId,
            HttpServletRequest request) {

        log.info("Closing poll with ID: {}", pollId);

        CreatePollResponse response = pollCreationService.closePoll(pollId, request);

        return ResponseEntity.ok(new ApiResponse("Poll closed successfully", response));
    }

    @PostMapping("/{pollId}/archive")
    public ResponseEntity<ApiResponse> archivePoll(
            @PathVariable Long pollId,
            HttpServletRequest request) {

        log.info("Archiving poll with ID: {}", pollId);

        pollCreationService.archivePoll(pollId, request);

        return ResponseEntity.ok(new ApiResponse("Poll archived successfully", null));
    }
}
