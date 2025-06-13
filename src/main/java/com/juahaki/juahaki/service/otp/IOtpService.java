package com.juahaki.juahaki.service.otp;

import com.juahaki.juahaki.enums.OtpType;
import com.juahaki.juahaki.model.user.User;

public interface IOtpService {

    void generateAndSendEmailVerificationOtp(User user);

    void generateAndSendPasswordResetOtp(String email);

    boolean verifyEmailVerificationOtp(String email, String otpCode);

    boolean verifyPasswordResetOtp(String email, String otpCode);

    void activateUserAccount(String email, String otpCode);

    void resendEmailVerificationOtp(String email);

    boolean isOtpValid(String email, String otpCode, OtpType type);
}