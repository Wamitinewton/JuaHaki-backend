package com.juahaki.juahaki.service.email;

import com.juahaki.juahaki.dto.email.EmailRequest;
import com.juahaki.juahaki.exception.EmailServiceException;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailSender {

    private final JavaMailSender mailSender;
    private final TemplateEngine templateEngine;

    @Value("${spring.mail.username}")
    private String fromEmail;

    @Value("${app.email.from-name:JuaHaki Civic Educator}")
    private String fromName;


    public void sendEmail(EmailRequest emailRequest) {
        try {
            log.debug("Preparing to send email to: {}", emailRequest.getTo());

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

            String content = processEmailContent(emailRequest);

            if (emailRequest.isHtml()) {
                helper.setText(generatePlainTextVersion(content), content);
            } else {
                helper.setText(content, false);
            }

            // Send the email
            mailSender.send(message);

            log.info("Email sent successfully to: {}", emailRequest.getTo());

        } catch (MessagingException e) {
            log.error("Failed to send email to {}: {}", emailRequest.getTo(), e.getMessage(), e);
            throw new EmailServiceException("Failed to send email: " + e.getMessage(), e);
        } catch (Exception e) {
            log.error("Unexpected error sending email to {}: {}", emailRequest.getTo(), e.getMessage(), e);
            throw new EmailServiceException("Failed to send email due to unexpected error", e);
        }
    }

    private String processEmailContent(EmailRequest emailRequest) {
        try {
            if (emailRequest.getTemplateName() != null && !emailRequest.getTemplateName().isEmpty()) {
                return processTemplate(emailRequest.getTemplateName(), emailRequest.getVariables());
            } else {
                return "Default email content - Please configure email template";
            }
        } catch (Exception e) {
            log.error("Failed to process email template: {}", e.getMessage(), e);
            return "Error processing email template. Please contact support.";
        }
    }

    private String processTemplate(String templateName, Map<String, Object> variables) {
        try {
            Context context = new Context();

            if (variables != null) {
                context.setVariables(variables);
            }


            String templatePath = "email/" + templateName;
            String processedTemplate = templateEngine.process(templatePath, context);

            log.debug("Successfully processed email template: {}", templateName);
            return processedTemplate;

        } catch (Exception e) {
            log.error("Failed to process template {}: {}", templateName, e.getMessage(), e);
            throw new EmailServiceException("Failed to process email template: " + templateName, e);
        }
    }


    private String generatePlainTextVersion(String htmlContent) {
        if (htmlContent == null || htmlContent.isEmpty()) {
            return "";
        }

        try {


            return htmlContent
                    .replaceAll("<br\\s*/?>", "\n")
                    .replaceAll("<p\\s*[^>]*>", "\n")
                    .replaceAll("</p>", "\n")
                    .replaceAll("<h[1-6]\\s*[^>]*>", "\n=== ")
                    .replaceAll("</h[1-6]>", " ===\n")
                    .replaceAll("<li\\s*[^>]*>", "\n• ")
                    .replaceAll("</li>", "")
                    .replaceAll("<[^>]+>", "")
                    .replaceAll("\\s+", " ")
                    .replaceAll("\n\\s*\n", "\n\n")
                    .trim();

        } catch (Exception e) {
            log.warn("Failed to generate plain text version: {}", e.getMessage());
            return "Please view this email in an HTML-capable email client.";
        }
    }

    /**
     * Validate email request
     */
    public void validateEmailRequest(EmailRequest emailRequest) {
        if (emailRequest == null) {
            throw new IllegalArgumentException("Email request cannot be null");
        }
        if (emailRequest.getTo() == null || emailRequest.getTo().trim().isEmpty()) {
            throw new IllegalArgumentException("Email recipient cannot be null or empty");
        }
        if (emailRequest.getSubject() == null || emailRequest.getSubject().trim().isEmpty()) {
            throw new IllegalArgumentException("Email subject cannot be null or empty");
        }
    }
}