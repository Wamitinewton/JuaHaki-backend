package com.juahaki.juahaki.core.poll.controller.admin;

import com.juahaki.juahaki.core.poll.model.Poll;
import com.juahaki.juahaki.core.poll.model.PollOpinion;
import com.juahaki.juahaki.core.poll.repository.PollOpinionRepository;
import com.juahaki.juahaki.core.poll.repository.PollRepository;
import com.juahaki.juahaki.core.poll.service.attachment.IPollAttachmentService;
import com.juahaki.juahaki.shared.dto.response.ApiResponse;
import com.juahaki.juahaki.shared.exception.CustomException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("${api.prefix}/admin/polls/attachments")
@RequiredArgsConstructor
@Slf4j
public class AdminPollAttachmentController {

    private final IPollAttachmentService pollAttachmentService;
    private final PollRepository pollRepository;
    private final PollOpinionRepository pollOpinionRepository;

    @DeleteMapping("/poll/{attachmentId}/force")
    public ResponseEntity<ApiResponse> forceDeletePollAttachment(@PathVariable Long attachmentId) {

        log.info("Admin force deleting poll attachment with ID: {}", attachmentId);

        boolean deleted = pollAttachmentService.deletePollAttachment(attachmentId);

        if (deleted) {
            return ResponseEntity.ok(new ApiResponse("Poll attachment force deleted successfully", null));
        } else {
            return ResponseEntity.badRequest()
                    .body(new ApiResponse("Failed to force delete poll attachment", null));
        }
    }

    @DeleteMapping("/opinion/{attachmentId}/force")
    public ResponseEntity<ApiResponse> forceDeleteOpinionAttachment(@PathVariable Long attachmentId) {

        log.info("Admin force deleting opinion attachment with ID: {}", attachmentId);

        boolean deleted = pollAttachmentService.deleteOpinionAttachment(attachmentId);

        if (deleted) {
            return ResponseEntity.ok(new ApiResponse("Opinion attachment force deleted successfully", null));
        } else {
            return ResponseEntity.badRequest()
                    .body(new ApiResponse("Failed to force delete opinion attachment", null));
        }
    }

    @DeleteMapping("/poll/{pollId}/all")
    public ResponseEntity<ApiResponse> deleteAllPollAttachments(@PathVariable Long pollId) {

        log.info("Admin deleting all attachments for poll ID: {}", pollId);

        Poll poll = pollRepository.findById(pollId)
                .orElseThrow(() -> new CustomException("Poll not found"));

        int deletedCount = pollAttachmentService.deleteAllPollAttachments(poll);

        return ResponseEntity.ok(new ApiResponse("All poll attachments deleted successfully",
                Map.of("deletedCount", deletedCount)));
    }

    @DeleteMapping("/opinion/{opinionId}/all")
    public ResponseEntity<ApiResponse> deleteAllOpinionAttachments(@PathVariable Long opinionId) {

        log.info("Admin deleting all attachments for opinion ID: {}", opinionId);

        PollOpinion opinion = pollOpinionRepository.findById(opinionId)
                .orElseThrow(() -> new CustomException("Opinion not found"));

        int deletedCount = pollAttachmentService.deleteAllOpinionAttachments(opinion);

        return ResponseEntity.ok(new ApiResponse("All opinion attachments deleted successfully",
                Map.of("deletedCount", deletedCount)));
    }
}
