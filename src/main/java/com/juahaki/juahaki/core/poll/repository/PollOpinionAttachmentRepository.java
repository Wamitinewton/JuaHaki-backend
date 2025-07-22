package com.juahaki.juahaki.core.poll.repository;

import com.juahaki.juahaki.core.poll.model.PollOpinion;
import com.juahaki.juahaki.core.poll.model.PollOpinionAttachment;
import com.juahaki.juahaki.shared.enums.AttachmentType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface PollOpinionAttachmentRepository extends JpaRepository<PollOpinionAttachment, Long> {

    // Basic queries
    List<PollOpinionAttachment> findByOpinion(PollOpinion opinion);

    List<PollOpinionAttachment> findByOpinionOrderByUploadedAtAsc(PollOpinion opinion);

    // Attachment by type
    List<PollOpinionAttachment> findByOpinionAndAttachmentType(PollOpinion opinion, AttachmentType attachmentType);

    long countByOpinionAndAttachmentType(PollOpinion opinion, AttachmentType attachmentType);

    // Count attachments
    long countByOpinion(PollOpinion opinion);

    // File operations
    boolean existsByStoragePath(String storagePath);

    @Query("SELECT poa FROM PollOpinionAttachment poa WHERE poa.storagePath = :storagePath")
    List<PollOpinionAttachment> findByStoragePath(@Param("storagePath") String storagePath);

    // Large files
    @Query("SELECT poa FROM PollOpinionAttachment poa WHERE poa.fileSize > :size")
    List<PollOpinionAttachment> findLargeFiles(@Param("size") Long size);

    // Recent uploads
    @Query("SELECT poa FROM PollOpinionAttachment poa WHERE poa.uploadedAt >= :date ORDER BY poa.uploadedAt DESC")
    List<PollOpinionAttachment> findRecentUploads(@Param("date") LocalDateTime date);

    @Query("SELECT SUM(poa.fileSize) FROM PollOpinionAttachment poa WHERE poa.opinion = :opinion")
    Long getTotalFileSizeForOpinion(@Param("opinion") PollOpinion opinion);

    @Query("SELECT poa.attachmentType, COUNT(poa), SUM(poa.fileSize) FROM PollOpinionAttachment poa GROUP BY poa.attachmentType")
    List<Object[]> getAttachmentStatsByType();

    // Opinions with attachments
    @Query("SELECT DISTINCT poa.opinion FROM PollOpinionAttachment poa WHERE poa.opinion.poll.id = :pollId")
    List<PollOpinion> findOpinionsWithAttachmentsByPoll(@Param("pollId") Long pollId);

    // Evidence attachments (images, documents)
    @Query("SELECT poa FROM PollOpinionAttachment poa WHERE poa.opinion.poll.id = :pollId AND poa.attachmentType IN ('IMAGE', 'DOCUMENT', 'PDF')")
    List<PollOpinionAttachment> findEvidenceAttachmentsByPoll(@Param("pollId") Long pollId);
}
