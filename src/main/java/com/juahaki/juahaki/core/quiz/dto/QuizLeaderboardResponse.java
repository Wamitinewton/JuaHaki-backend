package com.juahaki.juahaki.core.quiz.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class QuizLeaderboardResponse {
    private LocalDate quizDate;
    private String quizTitle;
    private int totalParticipants;
    private List<LeaderboardEntry> topPerformers;
    private LeaderboardEntry userRanking;
    private QuizStatistics statistics;
}
