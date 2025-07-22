package com.juahaki.juahaki.core.poll.dto.opinions;

import com.juahaki.juahaki.shared.enums.VoteChoice;
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
public class OpinionResponse {
    private Long id;
    private String content;
    private VoteChoice stance;
    private Boolean isAnonymous;
    private AuthorInfo author;
    private Long likesCount;
    private Long dislikesCount;
    private Double likePercentage;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private List<OpinionAttachmentInfo> attachments;
    private Boolean currentUserReaction;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AuthorInfo {
        private String username;
        private String firstName;
        private Boolean isRegistered;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OpinionAttachmentInfo {
        private Long id;
        private String fileName;
        private String fileUrl;
        private String attachmentType;
        private Long fileSize;
    }
}
