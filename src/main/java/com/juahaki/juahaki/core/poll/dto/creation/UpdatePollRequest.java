package com.juahaki.juahaki.core.poll.dto.creation;

import com.juahaki.juahaki.shared.enums.PollCategory;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdatePollRequest {

    @Size(max = 300, message = "Title must not exceed 300 characters")
    private String title;

    @Size(max = 2000, message = "Description must not exceed 2000 characters")
    private String description;

    private PollCategory category;

    private LocalDateTime startDate;

    private LocalDateTime endDate;

    private Boolean allowAnonymousVoting;

    private Boolean allowOpinions;
}
