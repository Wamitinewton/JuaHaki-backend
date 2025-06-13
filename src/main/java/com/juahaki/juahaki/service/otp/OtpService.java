package com.juahaki.juahaki.service.otp;

import java.time.LocalDateTime;
import java.util.Optional;
import com.juahaki.juahaki.enums.OtpType;
import com.juahaki.juahaki.exception.CustomException;
import com.juahaki.juahaki.model.otp.Otp;
import com.juahaki.juahaki.model.user.User;
import com.juahaki.juahaki.repository.otp.OtpRepository;
import com.juahaki.juahaki.repository.user.UserRepository;
import com.juahaki.juahaki.service.email.IEmailService;
import com.juahaki.juahaki.util.otp.OtpGenerator;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class OtpService implements IOtpService {

    private final OtpRepository otpRepository;
    private final UserRepository userRepository;
    private final IEmailService emailService;
    private final OtpGenerator otpGenerator;

    @Value("${app.otp.expiry-minutes:10}")
    private int otpExpiryminutes;

    @Value("${app.otp.max-attempts:5}")
    private int maxOtpAttempts;

    @Override
    @Transactional
    public void generateAndSendEmailVerificationOtp(User user) {
        validateUser(user);

        otpRepository.invalidateUserOtpsByType(user, OtpType.EMAIL_VERIFICATION);

        String otpCode = otpGenerator.generateOtp();

        Otp otp = createOtp(otpCode, user.getEmail(), user, OtpType.EMAIL_VERIFICATION);
        otpRepository.save(otp);

        try {
            emailService.sendSignUpOtp(user.getEmail(), otpCode, user.getFirstName());
        } catch (Exception e) {
            throw new CustomException("Failed to send verification email. Please try again.");
        }
    }

    @Override
    @Transactional
    public void generateAndSendPasswordResetOtp(String email) {
        if (!StringUtils.hasText(email)) {
            throw new IllegalArgumentException("Email is required");
        }

        User user = userRepository.findByEMail(email.toLowerCase().trim())
                .orElseThrow(() -> new CustomException("User not found with email: " + email));

        otpRepository.invalidateUserOtpsByType(user, OtpType.PASSWORD_RESET);

        String otpCode = otpGenerator.generateOtp();

        Otp otp = createOtp(otpCode, user.getEmail(), user, OtpType.PASSWORD_RESET);
        otpRepository.save(otp);

        try {
            emailService.sendForgotPasswordOtp(user.getEmail(), otpCode, user.getFirstName());
        } catch (Exception e) {
            throw new CustomException("Failed to send password reset email. Please try again.");
        }
    }

    @Override
    @Transactional
    public boolean verifyEmailVerificationOtp(String email, String otpCode) {
        return verifyOtp(email, otpCode, OtpType.EMAIL_VERIFICATION);
    }

    @Override
    @Transactional
    public boolean verifyPasswordResetOtp(String email, String otpCode) {
        return verifyOtp(email, otpCode, OtpType.PASSWORD_RESET);
    }

    @Override
    @Transactional
    public void activateUserAccount(String email, String otpCode) {
        if (!verifyEmailVerificationOtp(email, otpCode)) {
            throw new CustomException("Invalid or expired OTP");
        }

        User user = userRepository.findByEMail(email.toLowerCase().trim())
                .orElseThrow(() -> new CustomException("User not found"));

        if (user.getEmailVerified()) {
            throw new CustomException("Account is already verified");
        }

        user.setEmailVerified(true);
        user.setEnabled(true);
        userRepository.save(user);

        try {
            emailService.sendAccountActivationSuccess(user.getEmail(), user.getFirstName());
        } catch (Exception e) {

        }
    }

    @Override
    @Transactional
    public void resendEmailVerificationOtp(String email) {
        if (!StringUtils.hasText(email)) {
            throw new IllegalArgumentException("Email is required");
        }

        User user = userRepository.findByEMail(email.toLowerCase().trim())
                .orElseThrow(() -> new CustomException("User not found"));

        if (user.getEmailVerified()) {
            throw new CustomException("Account is already verified");
        }

        generateAndSendEmailVerificationOtp(user);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isOtpValid(String email, String otpCode, OtpType type) {
        if (!StringUtils.hasText(email) || !StringUtils.hasText(otpCode)) {
            return false;
        }

        if (!otpGenerator.isValidOtpFormat(otpCode)) {
            return false;
        }

        return otpRepository.findByOtpCodeAndEmailAndTypeAndIsUsedFalseAndIsExpiredFalse(
                otpCode.toUpperCase(), email.toLowerCase().trim(), type).isPresent();
    }

    @Scheduled(fixedRate = 300000)
    @Transactional
    public void cleanUpExpiredOtps() {
        try {
            otpRepository.markExpiredOtps(LocalDateTime.now());

            LocalDateTime cutOffDate = LocalDateTime.now().minusHours(24);
            otpRepository.deleteOldOtps(cutOffDate);
        } catch (Exception e) {
        }
    }

    private boolean verifyOtp(String email, String otpCode, OtpType type) {
        if (!StringUtils.hasText(email) || !StringUtils.hasText(otpCode)) {
            throw new IllegalArgumentException("Email and OTP code are required");
        }

        if (!otpGenerator.isValidOtpFormat(otpCode)) {
            throw new CustomException("Invalid OTP format");
        }

        String normalizedEmail = email.toLowerCase().trim();
        String normalizedOtp = otpCode.toUpperCase();

        Optional<Otp> otpOptional = otpRepository
                .findByOtpCodeAndEmailAndTypeAndIsUsedFalseAndIsExpiredFalse(normalizedOtp, normalizedEmail, type);

        if (otpOptional.isEmpty()) {
            throw new CustomException("Invalid or expired otp");
        }

        Otp otp = otpOptional.get();

        if (otp.isExpired()) {
            otp.setExpired(true);
            otpRepository.save(otp);
            throw new CustomException("Otp has expired");
        }

        if (otp.hasExceededMaxAttempts()) {
            otp.setExpired(true);
            otpRepository.save(otp);
            throw new CustomException("Maximum OTP attemps exceeded");
        }

        otp.markAsUsed();
        otp.incrementAttempt();
        otpRepository.save(otp);
        return true;
    }

    private Otp createOtp(String otpCode, String email, User user, OtpType type) {
        Otp otp = new Otp();
        otp.setOtpCode(otpCode.toUpperCase());
        otp.setEmail(email.toLowerCase().trim());
        otp.setUser(user);
        otp.setType(type);
        otp.setCreatedAt(LocalDateTime.now());
        otp.setExpiresAt(LocalDateTime.now().plusMinutes(otpExpiryminutes));

        return otp;
    }

    private void validateUser(User user) {
        if (user == null) {
            throw new IllegalArgumentException("User cannot be null");
        }
        if (!StringUtils.hasText(user.getEmail())) {
            throw new IllegalArgumentException("User emil cannot be empty");
        }
        if (!StringUtils.hasText(user.getFirstName())) {
            throw new IllegalArgumentException("User first name cannot be empty");
        }
    }

}

