package com.juahaki.juahaki.core.poll.dto.results;

import com.juahaki.juahaki.shared.enums.PollCategory;
import com.juahaki.juahaki.shared.enums.PollStatus;
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
public class PollSummaryResponse {
    private Long id;
    private String title;
    private String description;
    private PollCategory category;
    private PollStatus status;
    private Boolean isActive;
    private Boolean isExpired;
    private LocalDateTime startDate;
    private LocalDateTime endDate;
    private LocalDateTime createdAt;
    private CreatorInfo creator;
    private VotingResults votingResults;
    private OpinionsSummary opinionsSummary;
    private UserParticipation userParticipation;
    private List<AttachmentInfo> attachments;


    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CreatorInfo {
        private String username;
        private String firstName;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class VotingResults {
        private Long totalVotes;
        private Long yesVotes;
        private Long noVotes;
        private Long neutralVotes;
        private Double yesPercentage;
        private Double noPercentage;
        private Double neutralPercentage;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OpinionsSummary {
        private Long totalOpinions;
        private Long yesOpinions;
        private Long noOpinions;
        private Long neutralOpinions;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UserParticipation {
        private Boolean hasVoted;
        private VoteChoice userVote;
        private Boolean hasOpinion;
    }

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
