package com.juahaki.juahaki.core.poll.dto.creation;

import com.juahaki.juahaki.shared.enums.PollCategory;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreatePollResponse {
    private Long pollId;
    private String title;
    private String description;
    private PollCategory category;
    private LocalDateTime startDate;
    private LocalDateTime endDate;
    private Boolean allowAnonymousVoting;
    private Boolean allowOpinions;
    private LocalDateTime createDate;
    private String creatorUsername;
    private List<AttachmentInfo> attachments;


    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AttachmentInfo {
        private Long id;
        private String fileName;
        private String fileUrl;
        private String attachmentType;
        private Long fileSize;
    }
}
