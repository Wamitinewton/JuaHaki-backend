package com.juahaki.juahaki.core.otp.service;

import com.juahaki.juahaki.core.user.model.User;
import com.juahaki.juahaki.shared.enums.OtpType;

public interface IOtpService {

    void generateAndSendEmailVerificationOtp(User user);

    void generateAndSendPasswordResetOtp(String email);

    boolean verifyEmailVerificationOtp(String email, String otpCode);

    boolean verifyPasswordResetOtp(String email, String otpCode);

    void activateUserAccount(String email, String otpCode);

    void resendEmailVerificationOtp(String email);

    boolean isOtpValid(String email, String otpCode, OtpType type);
}