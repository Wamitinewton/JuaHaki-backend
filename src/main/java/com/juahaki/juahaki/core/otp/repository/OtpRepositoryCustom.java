package com.juahaki.juahaki.core.otp.repository;


import com.juahaki.juahaki.core.user.model.User;
import com.juahaki.juahaki.shared.enums.OtpType;

import java.time.LocalDateTime;

public interface OtpRepositoryCustom {
    void markExpiredOtps(LocalDateTime now);

    void invalidateUserOtpsByType(User user, OtpType type);

    void deleteOldOtps(LocalDateTime cutoffDate);
}