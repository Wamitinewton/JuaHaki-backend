package com.juahaki.juahaki.service.email;


import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

import com.juahaki.juahaki.dto.email.EmailRequest;
import com.juahaki.juahaki.exception.EmailServiceException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;


import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class EmailService implements IEmailService {

    private final JavaMailSender mailSender;
    private final TemplateEngine templateEngine;

    @Value("${spring.mail.username}")
    private String fromEmail;

    @Value("${app.email.from-name:Dream Shops}")
    private String fromName;

    @Value("${app.otp.expiry-minutes:10}")
    private int otpExpiryMinutes;

    @Override
    public void sendEmail(EmailRequest emailRequest) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromEmail, fromName);
            helper.setTo(emailRequest.getTo());
            helper.setSubject(emailRequest.getSubject());
            helper.setReplyTo(fromEmail);

            message.setHeader("X-Priority", "1");
            message.setHeader("X-MSMail-Priority", "High");
            message.setHeader("X-Mailer", "JuaHaki");
            message.setHeader("X-Auto-Response-Suppress", "OOF, AutoReply");

            String content;
            if (emailRequest.getTemplateName() != null && !emailRequest.getTemplateName().isEmpty()) {
                content = processTemplate(emailRequest.getTemplateName(), emailRequest.getVariables());
            } else {
                content = "Default email content";
            }

            if (emailRequest.isHtml()) {
                helper.setText(generatePlainTextVersion(content), content);
            } else {
                helper.setText(content, false);
            }

            mailSender.send(message);
        } catch (MessagingException e) {
            throw new EmailServiceException("Failed to send email: " + e.getMessage(), e);
        } catch (Exception e) {
            throw new EmailServiceException("Failed to send email due to unexpected error", e);
        }
    }

    @Override
    public void sendSignUpOtp(String email, String otp, String firstName) {
        Map<String, Object> variables = buildCommonVariables();
        variables.put("firstName", firstName);
        variables.put("otp", otp);
        variables.put("expiryMinutes", otpExpiryMinutes);

        EmailRequest emailRequest = EmailRequest.builder()
                .to(email)
                .subject("🔐 Verify Your Account - JuaHaki")
                .templateName("signup-otp")
                .variables(variables)
                .isHtml(true)
                .build();

        sendEmail(emailRequest);
    }

    @Override
    public void sendForgotPasswordOtp(String email, String otp, String firstName) {
        Map<String, Object> variables = buildCommonVariables();
        variables.put("firstName", firstName);
        variables.put("otp", otp);
        variables.put("expiryMinutes", otpExpiryMinutes);
        variables.put("timestamp", getCurrentTimestamp());

        EmailRequest emailRequest = EmailRequest.builder()
                .to(email)
                .subject("🔑 Reset Your Password - JuaHaki")
                .templateName("forgot-password-otp")
                .variables(variables)
                .isHtml(true)
                .build();

        sendEmail(emailRequest);
    }

    @Override
    public void sendPasswordResetSuccess(String email, String firstName) {
        Map<String, Object> variables = buildCommonVariables();
        variables.put("firstName", firstName);
        variables.put("timestamp", getCurrentTimestamp());

        EmailRequest emailRequest = EmailRequest.builder()
                .to(email)
                .subject("✅ Password Reset Successful - JuaHaki")
                .templateName("password-reset-success")
                .variables(variables)
                .isHtml(true)
                .build();

        sendEmail(emailRequest);
    }

    @Override
    public void sendWelcomeEmail(String email, String firstName) {
        Map<String, Object> variables = buildCommonVariables();
        variables.put("firstName", firstName);
        EmailRequest emailRequest = EmailRequest.builder()
                .to(email)
                .subject("🎉 Welcome to JuaHaki!")
                .templateName("welcome")
                .variables(variables)
                .isHtml(true)
                .build();

        sendEmail(emailRequest);
    }

    @Override
    public void sendAccountActivationSuccess(String email, String firstName) {
        Map<String, Object> variables = buildCommonVariables();
        variables.put("firstName", firstName);

        EmailRequest emailRequest = EmailRequest.builder()
                .to(email)
                .subject("✅ Account Activated - JuaHaki")
                .templateName("account-activation-success")
                .variables(variables)
                .isHtml(true)
                .build();

        sendEmail(emailRequest);
    }

    private String processTemplate(String templateName, Map<String, Object> variables) {
        Context context = new Context();
        if (variables != null) {
            context.setVariables(variables);
        }
        return templateEngine.process("email/" + templateName, context);
    }

    private String generatePlainTextVersion(String htmlContent) {
        return htmlContent
                .replaceAll("<[^>]+>", "")
                .replaceAll("\\s+", " ")
                .trim();
    }

    private Map<String, Object> buildCommonVariables() {
        Map<String, Object> variables = new HashMap<>();
        variables.put("supportEmail", fromEmail);
        variables.put("companyName", "JuaHaki Civic Educator");
        variables.put("currentYear", java.time.Year.now().getValue());
        return variables;
    }

    private String getCurrentTimestamp() {
        return LocalDateTime.now()
                .format(DateTimeFormatter.ofPattern("MMMM dd, yyyy 'at' hh:mm a"));
    }
}