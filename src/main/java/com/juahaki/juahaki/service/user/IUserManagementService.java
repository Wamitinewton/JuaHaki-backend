package com.juahaki.juahaki.service.user;

import com.juahaki.juahaki.dto.notification.NotificationPreferencesRequest;
import com.juahaki.juahaki.dto.notification.UpdateFcmTokenRequest;
import com.juahaki.juahaki.dto.user.UpdatePasswordRequest;
import com.juahaki.juahaki.dto.user.UpdateProfileRequest;
import com.juahaki.juahaki.dto.user.UserInfo;
import com.juahaki.juahaki.model.user.User;
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
