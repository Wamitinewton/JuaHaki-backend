package com.juahaki.juahaki.core.poll.model;

import com.juahaki.juahaki.shared.enums.AttachmentType;
import jakarta.persistence.*;
import lombok.*;
import net.minidev.json.annotate.JsonIgnore;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "poll_attachments", indexes = {
        @Index(name = "idx_poll_attachment_poll_id", columnList = "pollId"),
        @Index(name = "idx_poll_attachment_type", columnList = "attachmentType")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = false)
public class PollAttachment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pollId", nullable = false,
            foreignKey = @ForeignKey(name = "fk_poll_attachment_poll"))
    @JsonIgnore
    private Poll poll;

    @Column(nullable = false, length = 255)
    private String filaName;

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
