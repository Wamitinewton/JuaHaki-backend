package com.juahaki.juahaki.core.poll.dto.attachments;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PollAttachmentResponse {
    private Long id;
    private String fileName;
    private String fileUrl;
    private String attachmentType;
    private Long fileSize;
    private String mimeType;
    private LocalDateTime uploadedAt;
}
