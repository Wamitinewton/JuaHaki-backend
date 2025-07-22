package com.juahaki.juahaki.core.poll.dto.voting;

import com.juahaki.juahaki.shared.enums.VoteChoice;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SubmitVoteResponse {
    private Boolean success;
    private String message;
    private VoteChoice voteChoice;
    private Boolean isAnonymous;
    private LocalDateTime votedAt;
    private VoteResultsSummary currentResults;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class VoteResultsSummary {
        private Long totalVotes;
        private Long yesVotes;
        private Long noVotes;
        private Long neutralVotes;
        private Double yesPercentage;
        private Double noPercentage;
        private Double neutralPercentage;
    }
}
