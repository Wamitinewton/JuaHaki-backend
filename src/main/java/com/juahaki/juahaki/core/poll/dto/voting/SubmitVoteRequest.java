package com.juahaki.juahaki.core.poll.dto.voting;

import com.juahaki.juahaki.shared.enums.VoteChoice;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotNull;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SubmitVoteRequest {

    @NotNull(message = "Poll ID is required")
    private Long pollId;

    @NotNull(message = "Vote choice is required")
    private VoteChoice voteChoice;

    @Builder.Default
    private Boolean isAnonymous = false;
}
