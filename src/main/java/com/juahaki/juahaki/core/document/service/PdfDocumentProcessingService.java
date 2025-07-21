package com.juahaki.juahaki.core.document.service;

import com.juahaki.juahaki.infrastructure.messaging.storage.dto.firebase.FirebaseFileDto;
import com.juahaki.juahaki.infrastructure.messaging.storage.firebase.FirebaseStorageService;
import lombok.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.reader.pdf.PagePdfDocumentReader;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.UrlResource;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.net.URL;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class PdfDocumentProcessingService {

    private final VectorStore vectorStore;
    private final FirebaseStorageService firebaseStorageService;
    private final Set<String> processedDocuments = Collections.synchronizedSet(new HashSet<>());
    @Value("${app.document.processing.chunk-size:1000}")
    private int chunkSize;
    @Value("${app.document.processing.chunk-overlap:200}")
    private int chunkOverlap;
    @Value("${app.document.processing.max-file-size:50485760}")
    private long maxFileSize;
    @Value("${app.document.processing.supported-types:pdf}")
    private String supportedTypes;

    /**
     * Upload and process a single PDF document
     */
    public DocumentProcessingResult uploadAndProcessDocument(MultipartFile file,
                                                             Map<String, String> metadata) {
        log.info("Starting upload and processing of document: {}", file.getOriginalFilename());

        try {
            validateFile(file);

            FirebaseFileDto uploadResult = firebaseStorageService.uploadFile(file, null);
            log.info("Document uploaded to Firebase: {}", uploadResult.getStoragePath());

            DocumentProcessingResult result = processDocumentFromUrl(
                    uploadResult.getDownloadUrl(),
                    uploadResult.getStoragePath(),
                    metadata
            );

            log.info("Successfully processed document: {} with {} chunks",
                    file.getOriginalFilename(), result.getChunksProcessed());

            return result;

        } catch (Exception e) {
            log.error("Failed to upload and process document {}: {}",
                    file.getOriginalFilename(), e.getMessage(), e);
            throw new RuntimeException("Document processing failed: " + e.getMessage(), e);
        }
    }

    /**
     * Process a PDF document from a URL (e.g., Firebase download URL)
     */
    public DocumentProcessingResult processDocumentFromUrl(String downloadUrl,
                                                           String storagePath,
                                                           Map<String, String> metadata) {
        log.info("Processing document from URL: {}", storagePath);

        try {
            // Check if already processed
            if (processedDocuments.contains(storagePath)) {
                log.info("Document already processed: {}", storagePath);
                return DocumentProcessingResult.builder()
                        .success(false)
                        .message("Document already processed")
                        .storagePath(storagePath)
                        .build();
            }

            // Create UrlResource from download URL
            UrlResource resource = new UrlResource(new URL(downloadUrl));

            // Initialize PDF reader
            PagePdfDocumentReader pdfReader = new PagePdfDocumentReader(resource);

            // Read documents
            List<Document> documents = pdfReader.get();
            log.info("PDF reader extracted {} pages from document", documents.size());

            if (documents.isEmpty()) {
                throw new RuntimeException("No content extracted from PDF");
            }

            // Enhance documents with metadata
            List<Document> enhancedDocuments = enhanceDocumentsWithMetadata(documents, storagePath, metadata);

            // Split documents into chunks
            List<Document> chunks = splitDocuments(enhancedDocuments);
            log.info("Split documents into {} chunks", chunks.size());

            // Store in vector database
            vectorStore.add(chunks);
            log.info("Successfully stored {} chunks in vector database", chunks.size());

            // Mark as processed
            processedDocuments.add(storagePath);

            return DocumentProcessingResult.builder()
                    .success(true)
                    .message("Document processed successfully")
                    .storagePath(storagePath)
                    .originalPages(documents.size())
                    .chunksProcessed(chunks.size())
                    .processedAt(LocalDateTime.now())
                    .metadata(metadata)
                    .build();

        } catch (Exception e) {
            log.error("Failed to process document from URL {}: {}", downloadUrl, e.getMessage(), e);
            throw new RuntimeException("Document processing failed: " + e.getMessage(), e);
        }
    }

    /**
     * Process all documents from Firebase storage
     */
    @Async
    public CompletableFuture<BatchProcessingResult> processAllFirebaseDocuments() {
        log.info("Starting batch processing of all Firebase documents");

        try {
            List<FirebaseFileDto> documents = firebaseStorageService.getAllDocuments();
            log.info("Found {} documents in Firebase storage", documents.size());

            if (documents.isEmpty()) {
                return CompletableFuture.completedFuture(
                        BatchProcessingResult.builder()
                                .totalDocuments(0)
                                .successfullyProcessed(0)
                                .failed(0)
                                .message("No documents found in Firebase storage")
                                .build()
                );
            }

            List<DocumentProcessingResult> results = new ArrayList<>();
            int successful = 0;
            int failed = 0;

            for (FirebaseFileDto document : documents) {
                try {
                    Map<String, String> metadata = createDefaultMetadata(document.getStoragePath());
                    DocumentProcessingResult result = processDocumentFromUrl(
                            document.getDownloadUrl(),
                            document.getStoragePath(),
                            metadata
                    );

                    results.add(result);

                    if (result.isSuccess()) {
                        successful++;
                    } else {
                        failed++;
                    }

                    Thread.sleep(100);

                } catch (Exception e) {
                    log.error("Failed to process document {}: {}", document.getStoragePath(), e.getMessage());
                    failed++;

                    results.add(DocumentProcessingResult.builder()
                            .success(false)
                            .message("Processing failed: " + e.getMessage())
                            .storagePath(document.getStoragePath())
                            .processedAt(LocalDateTime.now())
                            .build());
                }
            }

            BatchProcessingResult batchResult = BatchProcessingResult.builder()
                    .totalDocuments(documents.size())
                    .successfullyProcessed(successful)
                    .failed(failed)
                    .results(results)
                    .completedAt(LocalDateTime.now())
                    .message(String.format("Batch processing completed: %d successful, %d failed", successful, failed))
                    .build();

            log.info("Batch processing completed: {} total, {} successful, {} failed",
                    documents.size(), successful, failed);

            return CompletableFuture.completedFuture(batchResult);

        } catch (Exception e) {
            log.error("Batch processing failed: {}", e.getMessage(), e);
            return CompletableFuture.completedFuture(
                    BatchProcessingResult.builder()
                            .totalDocuments(0)
                            .successfullyProcessed(0)
                            .failed(0)
                            .message("Batch processing failed: " + e.getMessage())
                            .completedAt(LocalDateTime.now())
                            .build()
            );
        }
    }


    public DocumentProcessingResult reprocessDocument(String storagePath) {
        log.info("Reprocessing document: {}", storagePath);

        try {
            processedDocuments.remove(storagePath);

            List<FirebaseFileDto> allDocuments = firebaseStorageService.getAllDocuments();
            Optional<FirebaseFileDto> document = allDocuments.stream()
                    .filter(doc -> doc.getStoragePath().equals(storagePath))
                    .findFirst();

            if (document.isEmpty()) {
                throw new RuntimeException("Document not found in Firebase storage: " + storagePath);
            }

            Map<String, String> metadata = createDefaultMetadata(storagePath);
            return processDocumentFromUrl(
                    document.get().getDownloadUrl(),
                    storagePath,
                    metadata
            );

        } catch (Exception e) {
            log.error("Failed to reprocess document {}: {}", storagePath, e.getMessage(), e);
            throw new RuntimeException("Document reprocessing failed: " + e.getMessage(), e);
        }
    }

    /**
     * Delete document from vector store and Firebase
     */
    public boolean deleteDocument(String storagePath) {
        log.info("Deleting document: {}", storagePath);

        try {

            boolean firebaseDeleted = firebaseStorageService.deleteFile(storagePath);

            processedDocuments.remove(storagePath);

            log.info("Document deletion completed for: {}, Firebase deleted: {}", storagePath, firebaseDeleted);
            return firebaseDeleted;

        } catch (Exception e) {
            log.error("Failed to delete document {}: {}", storagePath, e.getMessage(), e);
            return false;
        }
    }

    /**
     * Get processing statistics
     */
    public ProcessingStatistics getProcessingStatistics() {
        log.debug("Getting processing statistics");

        try {
            List<FirebaseFileDto> allDocuments = firebaseStorageService.getAllDocuments();
            int totalDocuments = allDocuments.size();
            int processedCount = processedDocuments.size();

            List<String> unprocessedDocuments = allDocuments.stream()
                    .map(FirebaseFileDto::getStoragePath)
                    .filter(path -> !processedDocuments.contains(path))
                    .collect(Collectors.toList());

            return ProcessingStatistics.builder()
                    .totalDocuments(totalDocuments)
                    .processedDocuments(processedCount)
                    .unprocessedDocuments(unprocessedDocuments.size())
                    .unprocessedPaths(unprocessedDocuments)
                    .processingRate(totalDocuments > 0 ? (processedCount * 100.0 / totalDocuments) : 0.0)
                    .lastUpdated(LocalDateTime.now())
                    .build();

        } catch (Exception e) {
            log.error("Failed to get processing statistics: {}", e.getMessage(), e);
            return ProcessingStatistics.builder()
                    .totalDocuments(0)
                    .processedDocuments(0)
                    .unprocessedDocuments(0)
                    .processingRate(0.0)
                    .lastUpdated(LocalDateTime.now())
                    .message("Failed to retrieve statistics: " + e.getMessage())
                    .build();
        }
    }

    /**
     * Search for documents in the vector store
     */
    public List<Document> searchDocuments(String query, int limit, double similarityThreshold) {
        log.debug("Searching documents with query: '{}', limit: {}, threshold: {}",
                query, limit, similarityThreshold);

        try {
            return vectorStore.similaritySearch(
                    org.springframework.ai.vectorstore.SearchRequest.builder()
                            .query(query)
                            .topK(limit)
                            .similarityThreshold(similarityThreshold)
                            .build()
            );

        } catch (Exception e) {
            log.error("Failed to search documents: {}", e.getMessage(), e);
            return Collections.emptyList();
        }
    }


    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("File cannot be null or empty");
        }

        if (file.getSize() > maxFileSize) {
            throw new IllegalArgumentException(
                    String.format("File size exceeds maximum allowed size of %d bytes", maxFileSize)
            );
        }

        String filename = file.getOriginalFilename();
        if (filename == null || !filename.toLowerCase().endsWith(".pdf")) {
            throw new IllegalArgumentException("Only PDF files are supported");
        }
    }

    private List<Document> enhanceDocumentsWithMetadata(List<Document> documents,
                                                        String storagePath,
                                                        Map<String, String> userMetadata) {
        List<Document> enhancedDocuments = new ArrayList<>();

        for (int i = 0; i < documents.size(); i++) {
            Document original = documents.get(i);

            // Create enhanced metadata
            Map<String, Object> enhancedMetadata = new HashMap<>(original.getMetadata());

            // Add default metadata
            enhancedMetadata.put("source", storagePath);
            enhancedMetadata.put("document_type", "pdf");
            enhancedMetadata.put("page_number", i + 1);
            enhancedMetadata.put("total_pages", documents.size());
            enhancedMetadata.put("processed_at", LocalDateTime.now().toString());
            enhancedMetadata.put("document_category", "civic_education");
            enhancedMetadata.put("language", "english");

            if (userMetadata != null) {
                enhancedMetadata.putAll(userMetadata);
            }

            String enhancedContent = String.format("Page %d of %d:\n%s",
                    i + 1, documents.size(), original.getText());

            Document enhancedDocument = new Document(enhancedContent, enhancedMetadata);
            enhancedDocuments.add(enhancedDocument);
        }

        return enhancedDocuments;
    }

    private List<Document> splitDocuments(List<Document> documents) {
        log.debug("Splitting {} documents into chunks", documents.size());

        // Configure text splitter if not already configured
        TokenTextSplitter splitter = new TokenTextSplitter(chunkSize, chunkOverlap, 5, 10000, true);

        List<Document> allChunks = new ArrayList<>();

        for (Document document : documents) {
            try {
                List<Document> chunks = splitter.split(document);

                // Add chunk-specific metadata
                for (int i = 0; i < chunks.size(); i++) {
                    Document chunk = chunks.get(i);
                    Map<String, Object> chunkMetadata = new HashMap<>(chunk.getMetadata());
                    chunkMetadata.put("chunk_index", i);
                    chunkMetadata.put("total_chunks_in_page", chunks.size());
                    chunkMetadata.put("chunk_id", UUID.randomUUID().toString());

                    Document enhancedChunk = new Document(Objects.requireNonNull(chunk.getText()), chunkMetadata);
                    allChunks.add(enhancedChunk);
                }

            } catch (Exception e) {
                log.error("Failed to split document: {}", e.getMessage());
                allChunks.add(document);
            }
        }

        log.debug("Split into {} total chunks", allChunks.size());
        return allChunks;
    }

    private Map<String, String> createDefaultMetadata(String storagePath) {
        Map<String, String> metadata = new HashMap<>();

        String filename = storagePath.substring(storagePath.lastIndexOf('/') + 1);
        metadata.put("filename", filename);
        metadata.put("upload_source", "firebase_storage");

        if (filename.toLowerCase().contains("constitution")) {
            metadata.put("primary_category", "Constitutional Law");
        } else if (filename.toLowerCase().contains("bill") || filename.toLowerCase().contains("law")) {
            metadata.put("primary_category", "Legislation");
        } else if (filename.toLowerCase().contains("budget") || filename.toLowerCase().contains("finance")) {
            metadata.put("primary_category", "Public Finance");
        } else {
            metadata.put("primary_category", "General Civic Education");
        }

        return metadata;
    }


    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class DocumentProcessingResult {
        private boolean success;
        private String message;
        private String storagePath;
        private int originalPages;
        private int chunksProcessed;
        private LocalDateTime processedAt;
        private Map<String, String> metadata;
        private List<String> errors;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class BatchProcessingResult {
        private int totalDocuments;
        private int successfullyProcessed;
        private int failed;
        private List<DocumentProcessingResult> results;
        private LocalDateTime completedAt;
        private String message;
        private long processingTimeMs;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ProcessingStatistics {
        private int totalDocuments;
        private int processedDocuments;
        private int unprocessedDocuments;
        private List<String> unprocessedPaths;
        private double processingRate;
        private LocalDateTime lastUpdated;
        private String message;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class DocumentSearchResult {
        private String content;
        private Map<String, Object> metadata;
        private double similarityScore;
        private String source;
        private int pageNumber;
    }
}
