package com.juahaki.juahaki.util.otp;

import org.springframework.stereotype.Component;

import java.security.SecureRandom;

@Component
public class OtpGenerator {

    private static final String CHARACTERS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
    private static final int OTP_LENGTH = 6;
    private static final SecureRandom random = new SecureRandom();

    public String generateOtp() {
        StringBuilder otp = new StringBuilder();

        for (int i = 0; i < OTP_LENGTH; i++) {
            int randomIndex = random.nextInt(CHARACTERS.length());
            otp.append(CHARACTERS.charAt(randomIndex));
        }
        return otp.toString();
    }

    public boolean isValidOtpFormat(String otp) {
        if (otp == null || otp.length() != OTP_LENGTH) {
            return false;
        }

        return otp.matches("^[A-Z0-9]{6}$");
    }
}
