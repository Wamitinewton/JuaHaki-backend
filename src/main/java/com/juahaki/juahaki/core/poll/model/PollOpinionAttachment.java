package com.juahaki.juahaki.core.poll.model;

import com.juahaki.juahaki.shared.enums.AttachmentType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "poll_opinion_attachments", indexes = {
        @Index(name = "idx_poll_opinion_attachment_opinion_id", columnList = "opinionId"),
        @Index(name = "idx_poll_opinion_attachment_type", columnList = "attachmentType")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = false)
public class PollOpinionAttachment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "opinionId", nullable = false,
            foreignKey = @ForeignKey(name = "fk_poll_opinion_attachment_opinion"))
    private PollOpinion opinion;

    @Column(nullable = false, length = 255)
    private String fileName;

    @Column(nullable = false, length = 500)
    private String fileUrl;

    @Column(nullable = false, length = 500)
    private String storagePath;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AttachmentType attachmentType;

    @Column(nullable = false)
    private Long fileSize;

    @Column(length = 100)
    private String mimeType;

    @CreationTimestamp
    @Column(nullable = false)
    private LocalDateTime uploadedAt;
}
