package com.juahaki.juahaki.core.poll.dto.opinions;

import com.juahaki.juahaki.shared.enums.ReactionType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OpinionReactionResponse {
    private ReactionType reactionType;
    private Long likesCount;
    private Long dislikesCount;
    private Double likePercentage;
}
