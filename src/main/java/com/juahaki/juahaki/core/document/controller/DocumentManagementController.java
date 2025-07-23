package com.juahaki.juahaki.core.document.controller;

import com.juahaki.juahaki.core.document.service.PdfDocumentProcessingService;
import com.juahaki.juahaki.shared.dto.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

@RestController
@RequestMapping("${api.prefix}/admin/document")
@RequiredArgsConstructor
@Slf4j
public class DocumentManagementController {

    private final PdfDocumentProcessingService pdfDocumentProcessingService;

    @PostMapping("/admin/documents/upload")
    public ResponseEntity<ApiResponse> uploadDocument(
            @RequestParam("file") MultipartFile file,
            @RequestParam(required = false) Map<String, String> metadata) {

        log.info("Admin uploading document: {}", file.getOriginalFilename());

        try {
            var result = pdfDocumentProcessingService.uploadAndProcessDocument(file, metadata);

            if (result.isSuccess()) {
                return ResponseEntity.ok(new ApiResponse("Document uploaded and processed successfully", result));
            } else {
                return ResponseEntity.badRequest().body(new ApiResponse(result.getMessage(), result));
            }
        } catch (Exception e) {
            log.error("Failed to upload document: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().body(new ApiResponse("Failed to upload document: " + e.getMessage(), null));
        }
    }

    @PostMapping("/admin/documents/process-all")
    public ResponseEntity<ApiResponse> processAllDocuments() {
        log.info("Admin initiating batch processing of all documents");

        try {
            CompletableFuture<PdfDocumentProcessingService.BatchProcessingResult> future =
                    pdfDocumentProcessingService.processAllFirebaseDocuments();

            return ResponseEntity.ok(new ApiResponse("Batch processing initiated",
                    Map.of("status", "processing", "message", "Processing will continue in background")));
        } catch (Exception e) {
            log.error("Failed to initiate batch processing: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().body(new ApiResponse("Failed to start batch processing: " + e.getMessage(), null));
        }
    }

    @PostMapping("/admin/documents/{storagePath}/reprocess")
    public ResponseEntity<ApiResponse> reprocessDocument(@PathVariable String storagePath) {
        log.info("Admin reprocessing document: {}", storagePath);

        try {
            var result = pdfDocumentProcessingService.reprocessDocument(storagePath);

            if (result.isSuccess()) {
                return ResponseEntity.ok(new ApiResponse("Document reprocessed successfully", result));
            } else {
                return ResponseEntity.badRequest().body(new ApiResponse(result.getMessage(), result));
            }
        } catch (Exception e) {
            log.error("Failed to reprocess document: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().body(new ApiResponse("Failed to reprocess document: " + e.getMessage(), null));
        }
    }

    @DeleteMapping("/admin/documents/{storagePath}")
    public ResponseEntity<ApiResponse> deleteDocument(@PathVariable String storagePath) {
        log.info("Admin deleting document: {}", storagePath);

        try {
            boolean deleted = pdfDocumentProcessingService.deleteDocument(storagePath);

            if (deleted) {
                return ResponseEntity.ok(new ApiResponse("Document deleted successfully",
                        Map.of("storagePath", storagePath, "deleted", true)));
            } else {
                return ResponseEntity.badRequest().body(new ApiResponse("Failed to delete document",
                        Map.of("storagePath", storagePath, "deleted", false)));
            }
        } catch (Exception e) {
            log.error("Failed to delete document: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().body(new ApiResponse("Failed to delete document: " + e.getMessage(), null));
        }
    }

    @GetMapping("/admin/documents/statistics")
    public ResponseEntity<ApiResponse> getDocumentStatistics() {
        log.info("Getting document processing statistics");

        var statistics = pdfDocumentProcessingService.getProcessingStatistics();
        return ResponseEntity.ok(new ApiResponse("Document statistics retrieved successfully", statistics));
    }

    @GetMapping("/admin/documents/search")
    public ResponseEntity<ApiResponse> searchDocuments(
            @RequestParam String query,
            @RequestParam(defaultValue = "10") int limit,
            @RequestParam(defaultValue = "0.6") double similarityThreshold) {

        log.info("Searching documents with query: '{}', limit: {}", query, limit);

        try {
            var results = pdfDocumentProcessingService.searchDocuments(query, limit, similarityThreshold);
            return ResponseEntity.ok(new ApiResponse("Document search completed",
                    Map.of("query", query, "results", results, "count", results.size())));
        } catch (Exception e) {
            log.error("Document search failed: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().body(new ApiResponse("Document search failed: " + e.getMessage(), null));
        }
    }

}
