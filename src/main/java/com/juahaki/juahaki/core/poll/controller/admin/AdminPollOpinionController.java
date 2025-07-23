package com.juahaki.juahaki.core.poll.controller.admin;

import com.juahaki.juahaki.core.poll.service.opinion.IPollOpinionService;
import com.juahaki.juahaki.shared.dto.response.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("${api.prefix}/admin/polls/opinions")
@RequiredArgsConstructor
@Slf4j
public class AdminPollOpinionController {

    private final IPollOpinionService pollOpinionService;

    @DeleteMapping("/{opinionId}")
    public ResponseEntity<ApiResponse> deleteOpinion(
            @PathVariable Long opinionId,
            HttpServletRequest request) {

        log.info("Admin deleting opinion with ID: {}", opinionId);

        pollOpinionService.deleteOpinion(opinionId, request);

        return ResponseEntity.ok(new ApiResponse("Opinion deleted successfully by admin", null));
    }

    @DeleteMapping("/{opinionId}/reaction")
    public ResponseEntity<ApiResponse> removeReaction(
            @PathVariable Long opinionId,
            HttpServletRequest request) {

        log.info("Admin removing reaction from opinion with ID: {}", opinionId);

        pollOpinionService.removeReaction(opinionId, request);

        return ResponseEntity.ok(new ApiResponse("Reaction removed successfully by admin", null));
    }
}