package com.juahaki.juahaki.core.poll.mapper;

import com.juahaki.juahaki.core.poll.dto.attachments.PollAttachmentResponse;
import com.juahaki.juahaki.core.poll.model.PollAttachment;
import com.juahaki.juahaki.core.poll.model.PollOpinionAttachment;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
public class PollAttachmentMapper {

    public PollAttachmentResponse toPollAttachmentResponse(PollAttachment attachment) {
        if (attachment == null) {
            return null;
        }

        return PollAttachmentResponse.builder()
                .id(attachment.getId())
                .fileName(attachment.getFilaName())
                .fileUrl(attachment.getFileUrl())
                .attachmentType(attachment.getAttachmentType().name())
                .fileSize(attachment.getFileSize())
                .mimeType(attachment.getMimeType())
                .uploadedAt(attachment.getUploadedAt())
                .build();
    }

    public List<PollAttachmentResponse> toPollAttachmentResponseList(List<PollAttachment> attachments) {
        return attachments.stream()
                .map(this::toPollAttachmentResponse)
                .collect(Collectors.toList());
    }

    public PollAttachmentResponse toOpinionAttachmentResponse(PollOpinionAttachment attachment) {
        if (attachment == null) {
            return null;
        }

        return PollAttachmentResponse.builder()
                .id(attachment.getId())
                .fileName(attachment.getFileName())
                .fileUrl(attachment.getFileUrl())
                .attachmentType(attachment.getAttachmentType().name())
                .fileSize(attachment.getFileSize())
                .mimeType(attachment.getMimeType())
                .uploadedAt(attachment.getUploadedAt())
                .build();
    }

    public List<PollAttachmentResponse> toOpinionAttachmentResponseList(List<PollOpinionAttachment> attachments) {
        return attachments.stream()
                .map(this::toOpinionAttachmentResponse)
                .collect(Collectors.toList());
    }
}
