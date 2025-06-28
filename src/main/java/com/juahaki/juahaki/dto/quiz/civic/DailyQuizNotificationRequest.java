package com.juahaki.juahaki.dto.quiz.civic;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class DailyQuizNotificationRequest {
    private String title;
    private String body;
    private String quizId;
    private String imageUrl;
    private boolean sendToAll;
    private String targetAudience;
}
