package com.juahaki.juahaki.dto.quiz.civic;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class LeaderboardEntry {
    private int rank;
    private String username;
    private String firstName;
    private int score;
    private String performanceLevel;
    private LocalDateTime completedAt;
    private Long durationSeconds;
    private String durationFormatted;
    private boolean isCurrentUser;
}
