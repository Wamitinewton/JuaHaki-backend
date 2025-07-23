package com.juahaki.juahaki.core.poll.controller;

import com.juahaki.juahaki.core.poll.model.Poll;
import com.juahaki.juahaki.core.poll.model.PollOpinion;
import com.juahaki.juahaki.core.poll.repository.PollOpinionRepository;
import com.juahaki.juahaki.core.poll.repository.PollRepository;
import com.juahaki.juahaki.core.poll.service.attachment.IPollAttachmentService;
import com.juahaki.juahaki.shared.dto.response.ApiResponse;
import com.juahaki.juahaki.shared.enums.AttachmentType;
import com.juahaki.juahaki.shared.exception.CustomException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("${api.prefix}/polls/attachments")
@RequiredArgsConstructor
@Slf4j
public class PollAttachmentController {

    private final IPollAttachmentService pollAttachmentService;
    private final PollRepository pollRepository;
    private final PollOpinionRepository pollOpinionRepository;

    @PostMapping(value = "/poll/{pollId}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse> uploadPollAttachments(
            @PathVariable Long pollId,
            @RequestParam("files") List<MultipartFile> files) {

        log.info("Uploading {} attachments for poll ID: {}", files.size(), pollId);

        Poll poll = pollRepository.findById(pollId)
                .orElseThrow(() -> new CustomException("Poll not found"));

        List<Object> attachments = pollAttachmentService.uploadPollAttachments(poll, files);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new ApiResponse("Poll attachments uploaded successfully", attachments));
    }

    @PostMapping(value = "/opinion/{opinionId}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse> uploadOpinionAttachments(
            @PathVariable Long opinionId,
            @RequestParam("files") List<MultipartFile> files) {

        log.info("Uploading {} attachments for opinion ID: {}", files.size(), opinionId);

        PollOpinion opinion = pollOpinionRepository.findById(opinionId)
                .orElseThrow(() -> new CustomException("Opinion not found"));

        List<Object> attachments = pollAttachmentService.uploadOpinionAttachments(opinion, files);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new ApiResponse("Opinion attachments uploaded successfully", attachments));
    }

    @DeleteMapping("/poll/{attachmentId}")
    public ResponseEntity<ApiResponse> deletePollAttachment(@PathVariable Long attachmentId) {

        log.info("Deleting poll attachment with ID: {}", attachmentId);

        boolean deleted = pollAttachmentService.deletePollAttachment(attachmentId);

        if (deleted) {
            return ResponseEntity.ok(new ApiResponse("Poll attachment deleted successfully", null));
        } else {
            return ResponseEntity.badRequest()
                    .body(new ApiResponse("Failed to delete poll attachment", null));
        }
    }

    @DeleteMapping("/opinion/{attachmentId}")
    public ResponseEntity<ApiResponse> deleteOpinionAttachment(@PathVariable Long attachmentId) {

        log.info("Deleting opinion attachment with ID: {}", attachmentId);

        boolean deleted = pollAttachmentService.deleteOpinionAttachment(attachmentId);

        if (deleted) {
            return ResponseEntity.ok(new ApiResponse("Opinion attachment deleted successfully", null));
        } else {
            return ResponseEntity.badRequest()
                    .body(new ApiResponse("Failed to delete opinion attachment", null));
        }
    }

    @GetMapping("/poll/{pollId}")
    public ResponseEntity<ApiResponse> getPollAttachments(@PathVariable Long pollId) {

        log.debug("Getting attachments for poll ID: {}", pollId);

        Poll poll = pollRepository.findById(pollId)
                .orElseThrow(() -> new CustomException("Poll not found"));

        List<Object> attachments = pollAttachmentService.getPollAttachments(poll);

        return ResponseEntity.ok(new ApiResponse("Poll attachments retrieved successfully", attachments));
    }

    @GetMapping("/opinion/{opinionId}")
    public ResponseEntity<ApiResponse> getOpinionAttachments(@PathVariable Long opinionId) {

        log.debug("Getting attachments for opinion ID: {}", opinionId);

        PollOpinion opinion = pollOpinionRepository.findById(opinionId)
                .orElseThrow(() -> new CustomException("Opinion not found"));

        List<Object> attachments = pollAttachmentService.getOpinionAttachments(opinion);

        return ResponseEntity.ok(new ApiResponse("Opinion attachments retrieved successfully", attachments));
    }

    @GetMapping("/poll/{pollId}/type/{attachmentType}")
    public ResponseEntity<ApiResponse> getPollAttachmentsByType(
            @PathVariable Long pollId,
            @PathVariable AttachmentType attachmentType) {

        log.debug("Getting {} attachments for poll ID: {}", attachmentType, pollId);

        Poll poll = pollRepository.findById(pollId)
                .orElseThrow(() -> new CustomException("Poll not found"));

        List<Object> attachments = pollAttachmentService.getPollAttachmentsByType(poll, attachmentType);

        return ResponseEntity.ok(new ApiResponse("Poll attachments by type retrieved successfully", attachments));
    }

    @GetMapping("/opinion/{opinionId}/type/{attachmentType}")
    public ResponseEntity<ApiResponse> getOpinionAttachmentsByType(
            @PathVariable Long opinionId,
            @PathVariable AttachmentType attachmentType) {

        log.debug("Getting {} attachments for opinion ID: {}", attachmentType, opinionId);

        PollOpinion opinion = pollOpinionRepository.findById(opinionId)
                .orElseThrow(() -> new CustomException("Opinion not found"));

        List<Object> attachments = pollAttachmentService.getOpinionAttachmentsByType(opinion, attachmentType);

        return ResponseEntity.ok(new ApiResponse("Opinion attachments by type retrieved successfully", attachments));
    }

    @GetMapping("/poll/{pollId}/statistics")
    public ResponseEntity<ApiResponse> getPollAttachmentStatistics(@PathVariable Long pollId) {

        log.debug("Getting attachment statistics for poll ID: {}", pollId);

        Poll poll = pollRepository.findById(pollId)
                .orElseThrow(() -> new CustomException("Poll not found"));

        Map<String, Object> statistics = pollAttachmentService.getPollAttachmentStatistics(poll);

        return ResponseEntity.ok(new ApiResponse("Poll attachment statistics retrieved successfully", statistics));
    }

    @GetMapping("/opinion/{opinionId}/statistics")
    public ResponseEntity<ApiResponse> getOpinionAttachmentStatistics(@PathVariable Long opinionId) {

        log.debug("Getting attachment statistics for opinion ID: {}", opinionId);

        PollOpinion opinion = pollOpinionRepository.findById(opinionId)
                .orElseThrow(() -> new CustomException("Opinion not found"));

        Map<String, Object> statistics = pollAttachmentService.getOpinionAttachmentStatistics(opinion);

        return ResponseEntity.ok(new ApiResponse("Opinion attachment statistics retrieved successfully", statistics));
    }

    @GetMapping("/limits/poll")
    public ResponseEntity<ApiResponse> getPollAttachmentLimits() {

        log.debug("Getting poll attachment limits");

        Map<String, Object> limits = Map.of(
                "maxFileSize", pollAttachmentService.getMaxPollAttachmentSize(),
                "maxFileCount", pollAttachmentService.getMaxPollAttachmentCount(),
                "supportedTypes", pollAttachmentService.getSupportedPollAttachmentTypes()
        );

        return ResponseEntity.ok(new ApiResponse("Poll attachment limits retrieved successfully", limits));
    }

    @GetMapping("/limits/opinion")
    public ResponseEntity<ApiResponse> getOpinionAttachmentLimits() {

        log.debug("Getting opinion attachment limits");

        Map<String, Object> limits = Map.of(
                "maxFileSize", pollAttachmentService.getMaxOpinionAttachmentSize(),
                "maxFileCount", pollAttachmentService.getMaxOpinionAttachmentCount(),
                "supportedTypes", pollAttachmentService.getSupportedOpinionAttachmentTypes()
        );

        return ResponseEntity.ok(new ApiResponse("Opinion attachment limits retrieved successfully", limits));
    }
}
