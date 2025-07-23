package com.juahaki.juahaki.core.poll.service.attachment;

import com.juahaki.juahaki.core.poll.model.Poll;
import com.juahaki.juahaki.core.poll.model.PollAttachment;
import com.juahaki.juahaki.core.poll.model.PollOpinion;
import com.juahaki.juahaki.core.poll.model.PollOpinionAttachment;
import com.juahaki.juahaki.core.poll.repository.PollAttachmentRepository;
import com.juahaki.juahaki.core.poll.repository.PollOpinionAttachmentRepository;
import com.juahaki.juahaki.infrastructure.storage.dto.s3.S3FileDto;
import com.juahaki.juahaki.infrastructure.storage.dto.s3.S3UploadRequest;
import com.juahaki.juahaki.infrastructure.storage.s3.IS3StorageService;
import com.juahaki.juahaki.shared.enums.AttachmentType;
import com.juahaki.juahaki.shared.exception.CustomException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class PollAttachmentService implements IPollAttachmentService{

    private final IS3StorageService s3StorageService;
    private final PollAttachmentRepository pollAttachmentRepository;
    private final PollOpinionAttachmentRepository pollOpinionAttachmentRepository;


    @Value("${app.poll.attachments.max-size:10485760}")
    private long maxPollAttachmentSize;

    @Value("${app.poll.attachments.max-count:5}")
    private int maxPollAttachmentCount;

    @Value("${app.opinion.attachments.max-size:5242880}")
    private long maxOpinionAttachmentSize;

    @Value("${app.opinion.attachments.max-count:3}")
    private int maxOpinionAttachmentCount;

    private static final List<String> SUPPORTED_POLL_TYPES = Arrays.asList(
            "image/jpeg", "image/png", "image/gif", "image/webp",
            "application/pdf", "text/plain",
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
            "application/msword"
    );

    private static final List<String> SUPPORTED_OPINION_TYPES = Arrays.asList(
            "image/jpeg", "image/png", "image/gif", "image/webp",
            "application/pdf"
    );


    @Override
    @Transactional
    public List<Object> uploadPollAttachments(Poll poll, List<MultipartFile> attachments) {
        if (poll == null) {
            throw new IllegalArgumentException("Poll cannot be null");
        }

        if (attachments == null || attachments.isEmpty()) {
            return Collections.emptyList();
        }

        validatePollAttachments(attachments);

        List<PollAttachment> pollAttachments = new ArrayList<>();

        for (MultipartFile file : attachments) {
            try {
                PollAttachment attachment = uploadSinglePollAttachment(poll, file);
                pollAttachments.add(attachment);
            } catch (Exception e) {
                log.error("Failed to upload poll attachment: {}", file.getOriginalFilename(), e);
                // Clean up any successfully uploaded files
                rollbackPollAttachments(pollAttachments);
                throw new CustomException("Failed to upload poll attachment: " + file.getOriginalFilename());
            }
        }

        log.info("Successfully uploaded {} poll attachments for poll ID: {}",
                pollAttachments.size(), poll.getId());

        return new ArrayList<>(pollAttachments);
    }

    @Override
    @Transactional
    public List<Object> uploadOpinionAttachments(PollOpinion opinion, List<MultipartFile> attachments) {
        if (opinion == null) {
            throw new IllegalArgumentException("Opinion cannot be null");
        }

        if (attachments == null || attachments.isEmpty()) {
            return Collections.emptyList();
        }

        validateOpinionAttachments(attachments);

        List<PollOpinionAttachment> opinionAttachments = new ArrayList<>();

        for (MultipartFile file : attachments) {
            try {
                PollOpinionAttachment attachment = uploadSingleOpinionAttachment(opinion, file);
                opinionAttachments.add(attachment);
            } catch (Exception e) {
                log.error("Failed to upload opinion attachment: {}", file.getOriginalFilename(), e);
                // Clean up any successfully uploaded files
                rollbackOpinionAttachments(opinionAttachments);
                throw new CustomException("Failed to upload opinion attachment: " + file.getOriginalFilename());
            }
        }

        log.info("Successfully uploaded {} opinion attachments for opinion ID: {}",
                opinionAttachments.size(), opinion.getId());

        return new ArrayList<>(opinionAttachments);
    }

    @Override
    public boolean deletePollAttachment(Long attachmentId) {
        if (attachmentId == null) {
            return false;
        }

        Optional<PollAttachment> attachmentOpt = pollAttachmentRepository.findById(attachmentId);
        if (attachmentOpt.isEmpty()) {
            return false;
        }

        PollAttachment attachment = attachmentOpt.get();

        try {
            // Delete from s3 first
            if (StringUtils.hasText(attachment.getStoragePath())) {
                boolean deleted = s3StorageService.deleteFile(attachment.getStoragePath());
                if (!deleted) {
                    log.warn("Failed to delete file from S3: {}", attachment.getStoragePath());
                }
            }

            // Delete from database
            pollAttachmentRepository.delete(attachment);

            log.info("Successfully deleted file from S3: {}", attachment.getStoragePath());
            return true;
        } catch (Exception e) {
            log.error("Failed to delete file from S3: {}", attachment.getStoragePath());
            return false;
        }
    }

    @Override
        public boolean deleteOpinionAttachment(Long attachmentId) {
        if (attachmentId == null) {
            return false;
        }

        Optional<PollOpinionAttachment> attachmentOpt = pollOpinionAttachmentRepository.findById(attachmentId);
        if (attachmentOpt.isEmpty()) {
            log.warn("Opinion attachment not found with ID: {}", attachmentId);
            return false;
        }

        PollOpinionAttachment attachment = attachmentOpt.get();

        try {
            // Delete from S3 first
            if (StringUtils.hasText(attachment.getStoragePath())) {
                boolean deleted = s3StorageService.deleteFile(attachment.getStoragePath());
                if (!deleted) {
                    log.warn("Failed to delete file from S3: {}", attachment.getStoragePath());
                }
            }

            // Delete from database
            pollOpinionAttachmentRepository.delete(attachment);

            log.info("Successfully deleted opinion attachment ID: {}", attachmentId);
            return true;
        } catch (Exception e) {
            log.error("Error deleting opinion attachment ID: {}", attachmentId, e);
            return false;
        }
    }

    @Override
    @Transactional
    public int deleteAllPollAttachments(Poll poll) {
        if (poll == null) {
            return 0;
        }

        List<PollAttachment> attachments = pollAttachmentRepository.findByPoll(poll);
        if (attachments.isEmpty()) {
            return 0;
        }

        int deletedCount = 0;
        for (PollAttachment attachment : attachments) {
            if (deletePollAttachment(attachment.getId())) {
                deletedCount++;
            }
        }

        log.info("Deleted {} poll attachments for poll ID: {}", deletedCount, poll.getId());
        return deletedCount;
    }

    @Override
    @Transactional
    public int deleteAllOpinionAttachments(PollOpinion opinion) {
        if (opinion == null) {
            return 0;
        }

        List<PollOpinionAttachment> attachments = pollOpinionAttachmentRepository.findByOpinion(opinion);
        if (attachments.isEmpty()) {
            return 0;
        }

        int deletedCount = 0;
        for (PollOpinionAttachment attachment : attachments) {
            if (deleteOpinionAttachment(attachment.getId())) {
                deletedCount++;
            }
        }

        log.info("Deleted {} opinion attachments for opinion ID: {}", deletedCount, opinion.getId());
        return deletedCount;
    }

    @Override
    public List<Object> getPollAttachments(Poll poll) {
        if (poll == null) {
            return Collections.emptyList();
        }

        List<PollAttachment> attachments = pollAttachmentRepository.findByPollOrderByUploadedAtAsc(poll);
        return new ArrayList<>(attachments);
    }

    @Override
    public List<Object> getOpinionAttachments(PollOpinion opinion) {
        if (opinion == null) {
            return Collections.emptyList();
        }

        List<PollOpinionAttachment> attachments = pollOpinionAttachmentRepository.findByOpinionOrderByUploadedAtAsc(opinion);
        return new ArrayList<>(attachments);
    }

    @Override
    public List<Object> getPollAttachmentsByType(Poll poll, AttachmentType attachmentType) {
        if (poll == null || attachmentType == null) {
            return Collections.emptyList();
        }

        List<PollAttachment> attachments = pollAttachmentRepository.findByPollAndAttachmentType(poll, attachmentType);
        return new ArrayList<>(attachments);
    }

    @Override
    public List<Object> getOpinionAttachmentsByType(PollOpinion opinion, AttachmentType attachmentType) {
        if (opinion == null || attachmentType == null) {
            return Collections.emptyList();
        }

        List<PollOpinionAttachment> attachments = pollOpinionAttachmentRepository.findByOpinionAndAttachmentType(opinion, attachmentType);
        return new ArrayList<>(attachments);
    }

    @Override
    public void validatePollAttachments(List<MultipartFile> attachments) {
        if (attachments == null || attachments.isEmpty()) {
            return;
        }

        if (attachments.size() > maxPollAttachmentCount) {
            throw new IllegalArgumentException(
                    String.format("Too many attachments. Maximum allowed: %d", maxPollAttachmentCount));
        }

        for (MultipartFile file : attachments) {
            validateSingleFile(file, maxPollAttachmentSize, SUPPORTED_POLL_TYPES, "poll");
        }
    }

    @Override
    public void validateOpinionAttachments(List<MultipartFile> attachments) {
        if (attachments == null || attachments.isEmpty()) {
            return;
        }

        if (attachments.size() > maxOpinionAttachmentCount) {
            throw new IllegalArgumentException(
                    String.format("Too many attachments. Maximum allowed: %d", maxOpinionAttachmentCount));
        }

        for (MultipartFile file : attachments) {
            validateSingleFile(file, maxOpinionAttachmentSize, SUPPORTED_OPINION_TYPES, "opinion");
        }
    }

    @Override
    public long getTotalPollAttachmentSize(Poll poll) {
        if (poll == null) {
            return 0L;
        }

        Long totalSize = pollAttachmentRepository.getTotalFileSizeForPoll(poll);
        return totalSize != null ? totalSize : 0L;
    }

    @Override
    public long getTotalOpinionAttachmentSize(PollOpinion opinion) {
        if (opinion == null) {
            return 0L;
        }

        Long totalSize = pollOpinionAttachmentRepository.getTotalFileSizeForOpinion(opinion);
        return totalSize != null ? totalSize : 0L;
    }

    @Override
    public Map<String, Object> getPollAttachmentStatistics(Poll poll) {
        if (poll == null) {
            return Collections.emptyMap();
        }

        Map<String, Object> stats = new HashMap<>();
        stats.put("totalCount", pollAttachmentRepository.countByPoll(poll));
        stats.put("totalSize", getTotalPollAttachmentSize(poll));

        for (AttachmentType type : AttachmentType.values()) {
            long count = pollAttachmentRepository.countByPollAndAttachmentType(poll, type);
            stats.put(type.name().toLowerCase() + "Count", count);
        }

        return stats;
    }

    @Override
    public Map<String, Object> getOpinionAttachmentStatistics(PollOpinion opinion) {
        if (opinion == null) {
            return Collections.emptyMap();
        }

        Map<String, Object> stats = new HashMap<>();
        stats.put("totalCount", pollOpinionAttachmentRepository.countByOpinion(opinion));
        stats.put("totalSize", getTotalOpinionAttachmentSize(opinion));

        for (AttachmentType type : AttachmentType.values()) {
            long count = pollOpinionAttachmentRepository.countByOpinionAndAttachmentType(opinion, type);
            stats.put(type.name().toLowerCase() + "Count", count);
        }

        return stats;
    }

    @Override
    public boolean hasPollAttachments(Poll poll) {
        if (poll == null) {
            return false;
        }
        return pollAttachmentRepository.countByPoll(poll) > 0;
    }

    @Override
    public boolean hasOpinionAttachments(PollOpinion opinion) {
        if (opinion == null) {
            return false;
        }
        return pollOpinionAttachmentRepository.countByOpinion(opinion) > 0;
    }


    @Override
    public long getMaxPollAttachmentSize() {
        return maxPollAttachmentSize;
    }

    @Override
    public long getMaxOpinionAttachmentSize() {
        return maxOpinionAttachmentSize;
    }

    @Override
    public int getMaxPollAttachmentCount() {
        return maxPollAttachmentCount;
    }

    @Override
    public int getMaxOpinionAttachmentCount() {
        return maxOpinionAttachmentCount;
    }

    @Override
    public List<String> getSupportedPollAttachmentTypes() {
        return new ArrayList<>(SUPPORTED_POLL_TYPES);
    }

    @Override
    public List<String> getSupportedOpinionAttachmentTypes() {
        return new ArrayList<>(SUPPORTED_OPINION_TYPES);
    }

    private PollAttachment uploadSinglePollAttachment(Poll poll, MultipartFile file) {
        S3UploadRequest uploadRequest = S3UploadRequest.of("polls/" + poll.getId() + "/attachments");
        S3FileDto s3File = s3StorageService.uploadFile(file, uploadRequest);

        PollAttachment attachment = PollAttachment.builder()
                .poll(poll)
                .filaName(file.getOriginalFilename())
                .fileUrl(s3File.getPublicUrl())
                .storagePath(s3File.getS3Key())
                .attachmentType(determineAttachmentType(file.getContentType()))
                .fileSize(file.getSize())
                .mimeType(file.getContentType())
                .build();

        return pollAttachmentRepository.save(attachment);
    }


    private PollOpinionAttachment uploadSingleOpinionAttachment(PollOpinion opinion, MultipartFile file) {
        S3UploadRequest uploadRequest = S3UploadRequest.of("polls/" + opinion.getPoll().getId() + "/opinions/" + opinion.getId() + "/attachments");
        S3FileDto s3File = s3StorageService.uploadFile(file, uploadRequest);

        PollOpinionAttachment attachment = PollOpinionAttachment.builder()
                .opinion(opinion)
                .fileName(file.getOriginalFilename())
                .fileUrl(s3File.getPublicUrl())
                .storagePath(s3File.getS3Key())
                .attachmentType(determineAttachmentType(file.getContentType()))
                .fileSize(file.getSize())
                .mimeType(file.getContentType())
                .build();

        return pollOpinionAttachmentRepository.save(attachment);
    }

    private void validateSingleFile(MultipartFile file, long maxSize, List<String> supportedTypes, String context) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("File cannot be null or empty");
        }

        if (file.getSize() > maxSize) {
            throw new IllegalArgumentException(
                    String.format("File size exceeds maximum allowed for %s attachments: %d bytes", context, maxSize));
        }

        if (!StringUtils.hasText(file.getContentType()) || !supportedTypes.contains(file.getContentType())) {
            throw new IllegalArgumentException(
                    String.format("Unsupported file type for %s attachments: %s", context, file.getContentType()));
        }

        if (!StringUtils.hasText(file.getOriginalFilename())) {
            throw new IllegalArgumentException("File must have a valid filename");
        }
    }

    private AttachmentType determineAttachmentType(String contentType) {
        if (contentType == null) {
            return AttachmentType.DOCUMENT;
        }

        if (contentType.startsWith("image/")) {
            return AttachmentType.IMAGE;
        } else if (contentType.equals("application/pdf")) {
            return AttachmentType.PDF;
        } else if (contentType.startsWith("video/")) {
            return AttachmentType.VIDEO;
        } else {
            return AttachmentType.DOCUMENT;
        }
    }

    private void rollbackPollAttachments(List<PollAttachment> attachments) {
        for (PollAttachment attachment : attachments) {
            try {
                if (attachment.getId() != null) {
                    deletePollAttachment(attachment.getId());
                } else if (StringUtils.hasText(attachment.getStoragePath())) {
                    s3StorageService.deleteFile(attachment.getStoragePath());
                }
            } catch (Exception e) {
                log.error("Failed to rollback poll attachment: {}", attachment.getFilaName(), e);
            }
        }
    }

    private void rollbackOpinionAttachments(List<PollOpinionAttachment> attachments) {
        for (PollOpinionAttachment attachment : attachments) {
            try {
                if (attachment.getId() != null) {
                    deleteOpinionAttachment(attachment.getId());
                } else if (StringUtils.hasText(attachment.getStoragePath())) {
                    s3StorageService.deleteFile(attachment.getStoragePath());
                }
            } catch (Exception e) {
                log.error("Failed to rollback opinion attachment: {}", attachment.getFileName(), e);
            }
        }
    }
}
