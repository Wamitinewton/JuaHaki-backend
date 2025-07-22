package com.juahaki.juahaki.core.poll.dto.opinions;

import com.juahaki.juahaki.shared.enums.ReactionType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotNull;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReactToOpinionRequest {

    @NotNull(message = "Opinion ID is required")
    private Long opinionId;

    @NotNull(message = "Reaction type is required")
    private ReactionType reactionType;

    @Builder.Default
    private Boolean isAnonymous = false;
}
