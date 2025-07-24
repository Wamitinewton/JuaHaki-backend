package com.juahaki.juahaki.core.poll.controller;

import com.juahaki.juahaki.core.poll.dto.filters.OpinionFilterRequest;
import com.juahaki.juahaki.core.poll.dto.opinions.OpinionReactionResponse;
import com.juahaki.juahaki.core.poll.dto.opinions.OpinionResponse;
import com.juahaki.juahaki.core.poll.dto.opinions.ReactToOpinionRequest;
import com.juahaki.juahaki.core.poll.dto.opinions.SubmitOpinionRequest;
import com.juahaki.juahaki.core.poll.service.opinion.IPollOpinionService;
import com.juahaki.juahaki.shared.dto.response.ApiResponse;
import com.juahaki.juahaki.shared.dto.response.PageResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("${api.prefix}/polls/opinions")
@RequiredArgsConstructor
@Slf4j
public class PollOpinionController {

    private final IPollOpinionService pollOpinionService;

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse> submitOpinion(
            @Valid @ModelAttribute SubmitOpinionRequest submitOpinionRequest,
            HttpServletRequest request) {

        log.info("Submitting opinion for poll ID: {}", submitOpinionRequest.getPollId());

        OpinionResponse response = pollOpinionService.submitOpinion(submitOpinionRequest, request);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new ApiResponse("Opinion submitted successfully", response));
    }

    @PutMapping(value = "/{opinionId}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse> updateOpinion(
            @PathVariable Long opinionId,
            @Valid @ModelAttribute SubmitOpinionRequest submitOpinionRequest,
            HttpServletRequest request) {

        log.info("Updating opinion with ID: {}", opinionId);

        OpinionResponse response = pollOpinionService.updateOpinion(opinionId, submitOpinionRequest, request);

        return ResponseEntity.ok(new ApiResponse("Opinion updated successfully", response));
    }

    @DeleteMapping("/{opinionId}")
    public ResponseEntity<ApiResponse> deleteOpinion(
            @PathVariable Long opinionId,
            HttpServletRequest request) {

        log.info("Deleting opinion with ID: {}", opinionId);

        pollOpinionService.deleteOpinion(opinionId, request);

        return ResponseEntity.ok(new ApiResponse("Opinion deleted successfully", null));
    }

    @PostMapping("/react")
    public ResponseEntity<ApiResponse> reactToOpinion(
            @Valid @RequestBody ReactToOpinionRequest reactRequest,
            HttpServletRequest request) {

        log.info("Reacting to opinion with ID: {}", reactRequest.getOpinionId());

        OpinionReactionResponse response = pollOpinionService.reactToOpinion(reactRequest, request);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new ApiResponse("Reaction submitted successfully", response));
    }

    @DeleteMapping("/{opinionId}/reaction")
    public ResponseEntity<ApiResponse> removeReaction(
            @PathVariable Long opinionId,
            HttpServletRequest request) {

        log.info("Removing reaction from opinion with ID: {}", opinionId);

        OpinionReactionResponse response = pollOpinionService.removeReaction(opinionId, request);

        return ResponseEntity.ok(new ApiResponse("Reaction removed successfully", response));
    }

    @GetMapping
    public ResponseEntity<ApiResponse> getFilteredOpinions(
            @ModelAttribute OpinionFilterRequest filterRequest,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable,
            HttpServletRequest request) {

        log.debug("Getting filtered opinions with filter: {}", filterRequest);

        Page<OpinionResponse> response = pollOpinionService.getFilteredOpinions(filterRequest, pageable, request);

        return ResponseEntity.ok(new ApiResponse("Opinions retrieved successfully", response));
    }

    @GetMapping("/top")
    public ResponseEntity<ApiResponse> getTopOpinions(
            @RequestParam Long pollId,
            @RequestParam(defaultValue = "10") int limit,
            HttpServletRequest request) {

        log.debug("Getting top {} opinions for poll ID: {}", limit, pollId);

        List<OpinionResponse> response = pollOpinionService.getTopOpinions(pollId, limit, request);

        return ResponseEntity.ok(new ApiResponse("Top opinions retrieved successfully", response));
    }

    @GetMapping("/recent")
    public ResponseEntity<ApiResponse> getRecentOpinions(
            @RequestParam Long pollId,
            @RequestParam(defaultValue = "10") int limit,
            HttpServletRequest request) {

        log.debug("Getting recent {} opinions for poll ID: {}", limit, pollId);

        List<OpinionResponse> response = pollOpinionService.getRecentOpinions(pollId, limit, request);

        return ResponseEntity.ok(new ApiResponse("Recent opinions retrieved successfully", response));
    }

    @GetMapping("/my-opinions")
    public ResponseEntity<ApiResponse> getUserOpinions(
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable,
            HttpServletRequest request) {

        log.debug("Getting user opinions");

        Page<OpinionResponse> response = pollOpinionService.getUserOpinions(pageable, request);

        return ResponseEntity.ok(new ApiResponse("User opinions retrieved successfully", response));
    }


    @GetMapping("/poll/{pollId}")
    public ResponseEntity<ApiResponse> getOpinionsByPollId(
            @PathVariable Long pollId,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable,
            HttpServletRequest request) {

        log.debug("Getting opinions for poll ID: {}", pollId);

        PageResponse<OpinionResponse> response = pollOpinionService.getOpinionsByPollId(pollId, pageable, request);

        return ResponseEntity.ok(new ApiResponse("Poll opinions retrieved successfully", response));
    }
}
