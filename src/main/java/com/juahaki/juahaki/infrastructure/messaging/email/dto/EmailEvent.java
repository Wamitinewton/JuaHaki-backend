package com.juahaki.juahaki.infrastructure.messaging.email.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.juahaki.juahaki.shared.enums.EmailType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class EmailEvent {

    private String eventId;
    private EmailType emailType;
    private String recipient;
    private String subject;
    private String templateName;
    private Map<String, Object> templateVariables;
    private boolean isHtml;
    private LocalDateTime createdAt;
    private String userId;
    private int retryCount;
    private LocalDateTime scheduledAt;

    public static EmailEvent signUpOtp(String recipient, String otp, String firstName, String userId) {
        Map<String, Object> variables = Map.of(
                "firstName", firstName,
                "otp", otp,
                "expiryMinutes", 10
        );

        return EmailEvent.builder()
                .eventId(generateEventId())
                .emailType(EmailType.SIGNUP_OTP)
                .recipient(recipient)
                .subject("🔐 Verify Your Account - JuaHaki")
                .templateName("signup-otp")
                .templateVariables(variables)
                .isHtml(true)
                .userId(userId)
                .retryCount(0)
                .createdAt(LocalDateTime.now())
                .build();
    }

    public static EmailEvent forgotPasswordOtp(String recipient, String otp, String firstName, String userId) {
        Map<String, Object> variables = Map.of(
                "firstName", firstName,
                "otp", otp,
                "expiryMinutes", 10,
                "timestamp", LocalDateTime.now().toString()
        );

        return EmailEvent.builder()
                .eventId(generateEventId())
                .emailType(EmailType.FORGOT_PASSWORD_OTP)
                .recipient(recipient)
                .subject("🔑 Reset Your Password - JuaHaki")
                .templateName("forgot-password-otp")
                .templateVariables(variables)
                .isHtml(true)
                .userId(userId)
                .retryCount(0)
                .createdAt(LocalDateTime.now())
                .build();
    }

    public static EmailEvent passwordResetSuccess(String recipient, String firstName, String userId) {
        Map<String, Object> variables = Map.of(
                "firstName", firstName,
                "timestamp", LocalDateTime.now().toString()
        );

        return EmailEvent.builder()
                .eventId(generateEventId())
                .emailType(EmailType.PASSWORD_RESET_SUCCESS)
                .recipient(recipient)
                .subject("✅ Password Reset Successful - JuaHaki")
                .templateName("password-reset-success")
                .templateVariables(variables)
                .isHtml(true)
                .userId(userId)
                .retryCount(0)
                .createdAt(LocalDateTime.now())
                .build();
    }

    public static EmailEvent welcome(String recipient, String firstName, String userId) {
        Map<String, Object> variables = Map.of(
                "firstName", firstName
        );

        return EmailEvent.builder()
                .eventId(generateEventId())
                .emailType(EmailType.WELCOME)
                .recipient(recipient)
                .subject("🎉 Welcome to JuaHaki!")
                .templateName("welcome")
                .templateVariables(variables)
                .isHtml(true)
                .userId(userId)
                .retryCount(0)
                .createdAt(LocalDateTime.now())
                .build();
    }

    public static EmailEvent accountActivationSuccess(String recipient, String firstName, String userId) {
        Map<String, Object> variables = Map.of(
                "firstName", firstName
        );

        return EmailEvent.builder()
                .eventId(generateEventId())
                .emailType(EmailType.ACCOUNT_ACTIVATION_SUCCESS)
                .recipient(recipient)
                .subject("✅ Account Activated - JuaHaki")
                .templateName("account-activation-success")
                .templateVariables(variables)
                .isHtml(true)
                .userId(userId)
                .retryCount(0)
                .createdAt(LocalDateTime.now())
                .build();
    }

    public static EmailEvent accountLocked(String recipient, String firstName, String userId) {
        Map<String, Object> variables = Map.of(
                "firstName", firstName,
                "timestamp", LocalDateTime.now().toString()
        );

        return EmailEvent.builder()
                .eventId(generateEventId())
                .emailType(EmailType.ACCOUNT_LOCKED)
                .recipient(recipient)
                .subject("🔒 Account Locked - JuaHaki")
                .templateName("account-locked")
                .templateVariables(variables)
                .isHtml(true)
                .userId(userId)
                .retryCount(0)
                .createdAt(LocalDateTime.now())
                .build();
    }

    public static EmailEvent accountUnlocked(String recipient, String firstName, String userId) {
        Map<String, Object> variables = Map.of(
                "firstName", firstName,
                "timestamp", LocalDateTime.now().toString()
        );

        return EmailEvent.builder()
                .eventId(generateEventId())
                .emailType(EmailType.ACCOUNT_UNLOCKED)
                .recipient(recipient)
                .subject("🔓 Account Unlocked - JuaHaki")
                .templateName("account-unlocked")
                .templateVariables(variables)
                .isHtml(true)
                .userId(userId)
                .retryCount(0)
                .createdAt(LocalDateTime.now())
                .build();
    }

    private static String generateEventId() {
        return "email_" + System.currentTimeMillis() + "_" +
                java.util.UUID.randomUUID().toString().substring(0, 8);
    }

    public void incrementRetryCount() {
        this.retryCount++;
    }
}
