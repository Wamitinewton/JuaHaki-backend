package com.juahaki.juahaki.infrastructure.messaging.email.service;

import com.juahaki.juahaki.infrastructure.messaging.email.dto.EmailEvent;
import com.juahaki.juahaki.shared.enums.EmailType;
import com.juahaki.juahaki.shared.enums.Role;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class AccountManagementEmailService implements IAccountManagementEmailService {

    private final EmailProducerService emailProducerService;


    @Override
    public void sendSignUpOtpAsync(String email, String otp, String firstName, String userId) {
        log.info("Queueing signup OTP email for user: {}", email);

        EmailEvent emailEvent = EmailEvent.signUpOtp(email, otp, firstName, userId);
        emailProducerService.sendEmailEvent(emailEvent);

        log.debug("Signup OTP email queued successfully: eventId={}", emailEvent.getEventId());
    }

    @Override
    public void sendForgotPasswordOtpAsync(String email, String otp, String firstName, String userId) {
        log.info("Queueing forgot password OTP email for user: {}", email);

        EmailEvent emailEvent = EmailEvent.forgotPasswordOtp(email, otp, firstName, userId);
        emailProducerService.sendEmailEvent(emailEvent);

        log.debug("Forgot password OTP email queued successfully: eventId={}", emailEvent.getEventId());
    }

    @Override
    public void sendPasswordResetSuccessAsync(String email, String firstName, String userId) {
        log.info("Queueing password reset success email for user: {}", email);

        EmailEvent emailEvent = EmailEvent.passwordResetSuccess(email, firstName, userId);
        emailProducerService.sendEmailEvent(emailEvent);

        log.debug("Password reset success email queued successfully: eventId={}", emailEvent.getEventId());
    }

    @Override
    public void sendWelcomeEmailAsync(String email, String firstName, String userId) {
        log.info("Queueing welcome email for user: {}", email);

        EmailEvent emailEvent = EmailEvent.welcome(email, firstName, userId);
        emailProducerService.sendEmailEvent(emailEvent);

        log.debug("Welcome email queued successfully: eventId={}", emailEvent.getEventId());
    }

    @Override
    public void sendAccountActivationSuccessAsync(String email, String firstName, String userId) {
        log.info("Queueing account activation success email for user: {}", email);

        EmailEvent emailEvent = EmailEvent.accountActivationSuccess(email, firstName, userId);
        emailProducerService.sendEmailEvent(emailEvent);

        log.debug("Account activation success email queued successfully: eventId={}", emailEvent.getEventId());
    }

    @Override
    public void sendAccountLockedNotificationAsync(String email, String firstName, String userId) {
        log.info("Queueing account locked notification email for user: {}", email);

        EmailEvent emailEvent = EmailEvent.accountLocked(email, firstName, userId);
        emailProducerService.sendEmailEvent(emailEvent);

        log.debug("Account locked notification email queued successfully: eventId={}", emailEvent.getEventId());
    }

    @Override
    public void sendAccountUnlockedNotificationAsync(String email, String firstName, String userId) {
        log.info("Queueing account unlocked notification email for user: {}", email);

        EmailEvent emailEvent = EmailEvent.accountUnlocked(email, firstName, userId);
        emailProducerService.sendEmailEvent(emailEvent);

        log.debug("Account unlocked notification email queued successfully: eventId={}", emailEvent.getEventId());
    }

    @Override
    public void sendRoleChangeNotificationAsync(String email, String firstName, Role oldRole, Role newRole, String userId) {
        log.info("Queueing role change notification email for user: {} ({}->{})", email, oldRole, newRole);

        Map<String, Object> variables = Map.of(
                "firstName", firstName,
                "oldRole", formatRoleName(oldRole),
                "newRole", formatRoleName(newRole),
                "timestamp", LocalDateTime.now().toString()
        );

        EmailEvent emailEvent = EmailEvent.builder()
                .eventId(generateEventId())
                .emailType(EmailType.ROLE_CHANGE)
                .recipient(email)
                .subject("👤 Account Role Updated - JuaHaki")
                .templateName("role-change")
                .templateVariables(variables)
                .isHtml(true)
                .userId(userId)
                .retryCount(0)
                .createdAt(LocalDateTime.now())
                .build();

        emailProducerService.sendEmailEvent(emailEvent);

        log.debug("Role change notification email queued successfully: eventId={}", emailEvent.getEventId());
    }

    private String formatRoleName(Role role) {
        if (role == null) return "Unknown";
        return role.name().toLowerCase().replace("_", " ");
    }

    private String generateEventId() {
        return "email_" + System.currentTimeMillis() + "_" +
                java.util.UUID.randomUUID().toString().substring(0, 8);
    }
}