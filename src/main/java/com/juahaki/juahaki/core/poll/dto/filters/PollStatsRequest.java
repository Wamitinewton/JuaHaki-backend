package com.juahaki.juahaki.core.poll.dto.filters;

import com.juahaki.juahaki.shared.enums.PollCategory;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PollStatsRequest {
    private PollCategory category;
    private LocalDateTime fromDate;
    private LocalDateTime toDate;
    private String creatorUsername;
}
