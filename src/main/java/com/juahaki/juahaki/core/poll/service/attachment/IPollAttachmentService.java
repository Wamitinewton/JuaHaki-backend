package com.juahaki.juahaki.core.poll.service.attachment;

import com.juahaki.juahaki.core.poll.model.Poll;
import com.juahaki.juahaki.core.poll.model.PollOpinion;
import com.juahaki.juahaki.shared.enums.AttachmentType;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

/**
 * Service interface for managing poll and opinion attachments.
 * Handles file upload, validation, storage, and deletion for poll-related attachments.
 * Supports both poll attachments (for context) and opinion attachments (for evidence).
 */
public interface IPollAttachmentService {

    /**
     * Upload and associate attachments with a poll during creation.
     * Validates file types, sizes, and stores them in appropriate storage.
     *
     * @param poll the poll entity to associate attachments with
     * @param attachments list of files to upload
     * @return list of created poll attachment entities
     * @throws IllegalArgumentException if files are invalid or exceed limits
     * @throws RuntimeException if upload fails
     */
    List<Object> uploadPollAttachments(Poll poll, List<MultipartFile> attachments);

    /**
     * Upload and associate attachments with an opinion for evidence or context.
     * Supports images, documents, and other evidence types.
     *
     * @param opinion the opinion entity to associate attachments with
     * @param attachments list of files to upload
     * @return list of created opinion attachment entities
     * @throws IllegalArgumentException if files are invalid or exceed limits
     * @throws RuntimeException if upload fails
     */
    List<Object> uploadOpinionAttachments(PollOpinion opinion, List<MultipartFile> attachments);

    /**
     * Delete a specific poll attachment by ID.
     * Removes from both database and storage.
     *
     * @param attachmentId the ID of the attachment to delete
     * @return true if deletion was successful, false otherwise
     */
    boolean deletePollAttachment(Long attachmentId);

    /**
     * Delete a specific opinion attachment by ID.
     * Removes from both database and storage.
     *
     * @param attachmentId the ID of the attachment to delete
     * @return true if deletion was successful, false otherwise
     */
    boolean deleteOpinionAttachment(Long attachmentId);

    /**
     * Delete all attachments associated with a poll.
     * Used when a poll is being deleted.
     *
     * @param poll the poll whose attachments should be deleted
     * @return number of attachments successfully deleted
     */
    int deleteAllPollAttachments(Poll poll);

    /**
     * Delete all attachments associated with an opinion.
     * Used when an opinion is being deleted.
     *
     * @param opinion the opinion whose attachments should be deleted
     * @return number of attachments successfully deleted
     */
    int deleteAllOpinionAttachments(PollOpinion opinion);

    /**
     * Get all attachments for a specific poll.
     *
     * @param poll the poll to get attachments for
     * @return list of poll attachments
     */
    List<Object> getPollAttachments(Poll poll);

    /**
     * Get all attachments for a specific opinion.
     *
     * @param opinion the opinion to get attachments for
     * @return list of opinion attachments
     */
    List<Object> getOpinionAttachments(PollOpinion opinion);

    /**
     * Get attachments by type for a poll (e.g., only images or only documents).
     *
     * @param poll the poll to get attachments for
     * @param attachmentType the type of attachments to filter by
     * @return list of attachments of the specified type
     */
    List<Object> getPollAttachmentsByType(Poll poll, AttachmentType attachmentType);

    /**
     * Get attachments by type for an opinion.
     *
     * @param opinion the opinion to get attachments for
     * @param attachmentType the type of attachments to filter by
     * @return list of attachments of the specified type
     */
    List<Object> getOpinionAttachmentsByType(PollOpinion opinion, AttachmentType attachmentType);

    /**
     * Validate a list of files for poll attachment upload.
     * Checks file types, sizes, and counts against configured limits.
     *
     * @param attachments list of files to validate
     * @throws IllegalArgumentException if validation fails with detailed error message
     */
    void validatePollAttachments(List<MultipartFile> attachments);

    /**
     * Validate a list of files for opinion attachment upload.
     * Checks file types, sizes, and counts against configured limits.
     *
     * @param attachments list of files to validate
     * @throws IllegalArgumentException if validation fails with detailed error message
     */
    void validateOpinionAttachments(List<MultipartFile> attachments);

    /**
     * Get total file size for all attachments in a poll.
     * Used for storage quota management.
     *
     * @param poll the poll to calculate total size for
     * @return total size in bytes
     */
    long getTotalPollAttachmentSize(Poll poll);

    /**
     * Get total file size for all attachments in an opinion.
     *
     * @param opinion the opinion to calculate total size for
     * @return total size in bytes
     */
    long getTotalOpinionAttachmentSize(PollOpinion opinion);

    /**
     * Get attachment statistics for a poll including counts by type and total size.
     *
     * @param poll the poll to get statistics for
     * @return map containing attachment statistics
     */
    Map<String, Object> getPollAttachmentStatistics(Poll poll);

    /**
     * Get attachment statistics for an opinion including counts by type and total size.
     *
     * @param opinion the opinion to get statistics for
     * @return map containing attachment statistics
     */
    Map<String, Object> getOpinionAttachmentStatistics(PollOpinion opinion);

    /**
     * Check if a poll has any attachments.
     *
     * @param poll the poll to check
     * @return true if poll has attachments, false otherwise
     */
    boolean hasPollAttachments(Poll poll);

    /**
     * Check if an opinion has any attachments.
     *
     * @param opinion the opinion to check
     * @return true if opinion has attachments, false otherwise
     */
    boolean hasOpinionAttachments(PollOpinion opinion);

    /**
     * Get the maximum allowed file size for poll attachments.
     *
     * @return maximum file size in bytes
     */
    long getMaxPollAttachmentSize();

    /**
     * Get the maximum allowed file size for opinion attachments.
     *
     * @return maximum file size in bytes
     */
    long getMaxOpinionAttachmentSize();

    /**
     * Get the maximum number of attachments allowed per poll.
     *
     * @return maximum attachment count
     */
    int getMaxPollAttachmentCount();

    /**
     * Get the maximum number of attachments allowed per opinion.
     *
     * @return maximum attachment count
     */
    int getMaxOpinionAttachmentCount();

    /**
     * Get supported file types for poll attachments.
     *
     * @return list of supported MIME types
     */
    List<String> getSupportedPollAttachmentTypes();

    /**
     * Get supported file types for opinion attachments.
     *
     * @return list of supported MIME types
     */
    List<String> getSupportedOpinionAttachmentTypes();
}
