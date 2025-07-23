package com.juahaki.juahaki.core.user.service;

import com.juahaki.juahaki.core.user.dto.UpdatePasswordRequest;
import com.juahaki.juahaki.core.user.dto.UpdateProfileRequest;
import com.juahaki.juahaki.core.user.dto.UserInfo;
import com.juahaki.juahaki.core.user.dto.notification.NotificationPreferencesRequest;
import com.juahaki.juahaki.core.user.dto.notification.UpdateFcmTokenRequest;
import com.juahaki.juahaki.core.user.model.User;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.transaction.annotation.Transactional;

public interface IUserManagementService {
    UserInfo updatePassword(HttpServletRequest request, UpdatePasswordRequest updatePasswordRequest);

    UserInfo updateProfile(HttpServletRequest request, UpdateProfileRequest updateProfileRequest);

    void validatePasswordUpdateRequest(User user, UpdatePasswordRequest request);

    void validateProfileUpdateRequest(User user, UpdateProfileRequest request);

    boolean isPasswordTooSimilar(String currentPassword, String newPassword);

    void initiatePasswordReset(String email);

    void resetPassword(String email, String otp, String newPassword);

    UserInfo getUserById(HttpServletRequest request);

    void deleteUser(HttpServletRequest request);

    @Transactional
    UserInfo updateFcmToken(HttpServletRequest request, UpdateFcmTokenRequest updateRequest);

    @Transactional
    UserInfo updateNotificationPreferences(HttpServletRequest request,
                                           NotificationPreferencesRequest preferencesRequest);

    @Transactional
    void clearFcmToken(HttpServletRequest request);
}
