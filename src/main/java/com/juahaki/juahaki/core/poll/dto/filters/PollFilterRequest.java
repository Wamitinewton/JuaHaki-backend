package com.juahaki.juahaki.core.poll.dto.filters;

import com.juahaki.juahaki.shared.enums.PollCategory;
import com.juahaki.juahaki.shared.enums.PollStatus;
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
public class PollFilterRequest {
    private PollCategory category;
    private PollStatus status;
    private Boolean isActive;
    private String searchTerm;
    private LocalDateTime createdAfter;
    private LocalDateTime createdBefore;
    private LocalDateTime startDateAfter;
    private LocalDateTime startDateBefore;
    private String creatorUsername;
    private Boolean userHasVoted;
    private VoteChoice userVoteChoice;
    private Boolean allowAnonymousVoting;
    private Boolean allowOpinions;
}
