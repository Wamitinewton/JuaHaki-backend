package com.juahaki.juahaki.service.email;

import com.juahaki.juahaki.dto.email.EmailRequest;
import com.juahaki.juahaki.enums.Role;

public interface IEmailService {

    public void sendEmail(EmailRequest emailRequest);

    /**
     * Send OTP for user sign up verification
     *
     * @param email     User's email address
     * @param otp       Generated OTP code
     * @param firstName User's first name (optional)
     */
    void sendSignUpOtp(String email, String otp, String firstName);

    /**
     * Send OTP for forgot password verification
     *
     * @param email     User's email address
     * @param otp       Generated OTP code
     * @param firstName User's first name (optional)
     */
    void sendForgotPasswordOtp(String email, String otp, String firstName);

    /**
     * Send notification when password reset is successful
     *
     * @param email     User's email address
     * @param firstName User's first name (optional)
     */
    void sendPasswordResetSuccess(String email, String firstName);

    /**
     * Send welcome email after successful account activation
     *
     * @param email     User's email address
     * @param firstName User's first name (optional)
     */
    void sendWelcomeEmail(String email, String firstName);

    /**
     * Send confirmation when account is successfully activated
     *
     * @param email     User's email address
     * @param firstName User's first name (optional)
     */
    void sendAccountActivationSuccess(String email, String firstName);

    void sendAccountLockedNotification(String email, String firstName);

    void sendAccountUnlockedNotification(String email, String firstName);

    void sendRoleChangeNotification(String email, String firstName, Role oldRole, Role newRole);
}
