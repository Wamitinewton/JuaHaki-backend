package com.juahaki.juahaki.repository.otp;

import com.juahaki.juahaki.enums.OtpType;
import com.juahaki.juahaki.model.user.User;

import java.time.LocalDateTime;

public interface OtpRepositoryCustom {
    void markExpiredOtps(LocalDateTime now);
    void invalidateUserOtpsByType(User user, OtpType type);
    void deleteOldOtps(LocalDateTime cutoffDate);
}