package com.juahaki.juahaki.core.user.dto.admin;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class UserStatsResponse {
    private long totalUsers;
    private long activeUsers;
    private long inactiveUsers;
    private long lockedUsers;
    private long unverifiedUsers;
    private long adminUsers;
    private long regularUsers;
    private long oauthUsers;
    private long localUsers;
    private long usersCreatedThisMonth;
    private long usersCreatedThisWeek;
    private long usersCreatedToday;
}