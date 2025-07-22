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
public class VoteStatusResponse {
    private Boolean hasVoted;
    private VoteChoice userVote;
    private Boolean isAnonymous;
    private LocalDateTime votedAt;
}
