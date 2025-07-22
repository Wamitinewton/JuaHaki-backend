package com.juahaki.juahaki.core.poll.repository;

import com.juahaki.juahaki.core.poll.model.Poll;
import com.juahaki.juahaki.core.poll.model.PollAttachment;
import com.juahaki.juahaki.shared.enums.AttachmentType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface PollAttachmentRepository extends JpaRepository<PollAttachment, Long> {

    // Basic queries
    List<PollAttachment> findByPoll(Poll poll);

    List<PollAttachment> findByPollOrderByUploadedAtAsc(Poll poll);

    // Attachment by type
    List<PollAttachment> findByPollAndAttachmentType(Poll poll, AttachmentType attachmentType);

    long countByPollAndAttachmentType(Poll poll, AttachmentType attachmentType);

    // Count attachments
    long countByPoll(Poll poll);

    // File operations
    boolean existsByStoragePath(String storagePath);

    @Query("SELECT pa FROM PollAttachment pa WHERE pa.storagePath = :storagePath")
    List<PollAttachment> findByStoragePath(@Param("storagePath") String storagePath);

    // Large files
    @Query("SELECT pa FROM PollAttachment pa WHERE pa.fileSize > :size")
    List<PollAttachment> findLargeFiles(@Param("size") Long size);

    // Recent uploads
    @Query("SELECT pa FROM PollAttachment pa WHERE pa.uploadedAt >= :date ORDER BY pa.uploadedAt DESC")
    List<PollAttachment> findRecentUploads(@Param("date") LocalDateTime date);

    // Storage statistics
    @Query("SELECT SUM(pa.fileSize) FROM PollAttachment pa WHERE pa.poll = :poll")
    Long getTotalFileSizeForPoll(@Param("poll") Poll poll);

    @Query("SELECT pa.attachmentType, COUNT(pa), SUM(pa.fileSize) FROM PollAttachment pa GROUP BY pa.attachmentType")
    List<Object[]> getAttachmentStatsByType();
}
