package com.juahaki.juahaki.core.poll.dto.filters;

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
public class OpinionFilterRequest {
    private Long pollId;
    private VoteChoice stance;
    private Boolean isAnonymous;
    private Boolean fromRegisteredUsers;
    private String searchTerm;
    private LocalDateTime createdAfter;
    private LocalDateTime createdBefore;
    private Integer minLikes;
    private String sortBy;
}
