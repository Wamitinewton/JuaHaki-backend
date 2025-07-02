package com.juahaki.juahaki.service.email;

import com.juahaki.juahaki.dto.email.EmailRequest;
import com.juahaki.juahaki.enums.Role;

public interface IEmailService {


    void sendSignUpOtpAsync(String email, String otp, String firstName, String userId);


    void sendForgotPasswordOtpAsync(String email, String otp, String firstName, String userId);


    void sendPasswordResetSuccessAsync(String email, String firstName, String userId);


    void sendWelcomeEmailAsync(String email, String firstName, String userId);


    void sendAccountActivationSuccessAsync(String email, String firstName, String userId);


    void sendAccountLockedNotificationAsync(String email, String firstName, String userId);


    void sendAccountUnlockedNotificationAsync(String email, String firstName, String userId);


    void sendRoleChangeNotificationAsync(String email, String firstName, Role oldRole, Role newRole, String userId);
}
