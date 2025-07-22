package com.juahaki.juahaki.core.poll.dto.results;

import com.juahaki.juahaki.shared.enums.PollCategory;
import com.juahaki.juahaki.shared.enums.PollStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PollListResponse {
    private Long id;
    private String title;
    private String description;
    private PollCategory category;
    private PollStatus status;
    private Boolean isActive;
    private LocalDateTime startDate;
    private LocalDateTime endDate;
    private LocalDateTime createdAt;
    private String creatorUsername;
    private Long totalVotes;
    private Long totalOpinions;
    private Double yesPercentage;
    private Boolean userHasVoted;
}
