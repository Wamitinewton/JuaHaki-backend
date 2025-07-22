package com.juahaki.juahaki.core.poll.dto.results;

import com.juahaki.juahaki.core.poll.dto.opinions.OpinionResponse;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PollDetailsResponse {
    private PollSummaryResponse poll;
    private List<OpinionResponse> topOpinions;
    private List<OpinionResponse> recentOpinions;
    private Integer totalOpinionsCount;
    private Boolean canVote;
    private Boolean canComment;

}
