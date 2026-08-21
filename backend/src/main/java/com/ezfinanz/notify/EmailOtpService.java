package com.ezfinanz.notify;

import com.ezfinanz.common.ApiException;
import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

/**
 * Sends transactional emails for OTP verification, support requests, and application review updates.
 */
@Service
public class EmailOtpService {

    private static final Logger log = LoggerFactory.getLogger(EmailOtpService.class);

    private final JavaMailSender mailSender;
    private final String fromAddress;

    public EmailOtpService(
            JavaMailSender mailSender,
            @Value("${spring.mail.username}") String fromAddress
    ) {
        this.mailSender = mailSender;
        this.fromAddress = fromAddress;
    }

    /** Emails a one-time verification code to the given address. */
    public void sendOtp(String email, String code, int ttlMinutes) {
        try {
            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, false, "UTF-8");
            helper.setFrom(fromAddress, "EZFINANZ");
            helper.setTo(email);
            helper.setSubject("Your EZFINANZ verification code");
            helper.setText(
                    "Your EZFINANZ verification code is " + code + ".\n\n"
                            + "This code expires in " + ttlMinutes + " minutes.\n\n"
                            + "If you did not request this, you can ignore this email.",
                    false
            );
            mailSender.send(mimeMessage);
            log.info("OTP email accepted by SMTP for {}", email);
        } catch (MailException | jakarta.mail.MessagingException | java.io.UnsupportedEncodingException ex) {
            log.error("Failed to send OTP email to {}: {}", email, ex.getMessage(), ex);
            throw new ApiException(
                    HttpStatus.BAD_GATEWAY,
                    "EMAIL_SEND_FAILED",
                    "Could not send the verification email: " + rootMessage(ex)
            );
        }
    }

    /** Notifies the support mailbox about a customer Help & Support message. */
    public void sendSupportRequest(
            String notifyEmail,
            String customerName,
            String customerEmail,
            String customerPhone,
            Long applicationId,
            String subject,
            String message
    ) {
        try {
            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, false, "UTF-8");
            helper.setFrom(fromAddress, "EZFINANZ Support");
            helper.setTo(notifyEmail);
            helper.setReplyTo(customerEmail != null && !customerEmail.isBlank() ? customerEmail : fromAddress);
            helper.setSubject("[EZFINANZ Support] " + subject);
            helper.setText(
                    "A customer submitted a Help & Support message.\n\n"
                            + "Applicant: " + safe(customerName) + "\n"
                            + "Email: " + safe(customerEmail) + "\n"
                            + "Phone: " + safe(customerPhone) + "\n"
                            + "Application ID: EZF" + String.format("%09d", applicationId) + "\n\n"
                            + "Subject: " + subject + "\n\n"
                            + "Message:\n" + message + "\n",
                    false
            );
            mailSender.send(mimeMessage);
            log.info("Support notification email sent to {}", notifyEmail);
        } catch (MailException | jakarta.mail.MessagingException | java.io.UnsupportedEncodingException ex) {
            log.error("Failed to send support email to {}: {}", notifyEmail, ex.getMessage(), ex);
            throw new ApiException(
                    HttpStatus.BAD_GATEWAY,
                    "EMAIL_SEND_FAILED",
                    "Your message was saved, but we could not notify support by email. Please try again shortly."
            );
        }
    }

    private static String safe(String value) {
        return value == null || value.isBlank() ? "—" : value.trim();
    }

    /** Emails the applicant after an admin approve/reject decision. */
    public void sendApplicationReviewUpdate(
            String email,
            String applicantName,
            Long applicationId,
            String statusLabel,
            String adminMessage
    ) {
        if (email == null || email.isBlank()) {
            throw new ApiException(
                    HttpStatus.BAD_REQUEST,
                    "EMAIL_REQUIRED",
                    "This applicant has no email address on file."
            );
        }
        try {
            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, false, "UTF-8");
            helper.setFrom(fromAddress, "EZFINANZ");
            helper.setTo(email.trim());
            helper.setSubject("EZFINANZ application update: " + statusLabel);
            String messageBody = adminMessage == null || adminMessage.isBlank()
                    ? "No additional message was provided."
                    : adminMessage.trim();
            helper.setText(
                    "Hello " + safe(applicantName) + ",\n\n"
                            + "Your EZFINANZ loan application (ID: EZF" + String.format("%09d", applicationId) + ") has been reviewed.\n\n"
                            + "Status: " + statusLabel + "\n\n"
                            + "Message from the review team:\n"
                            + messageBody + "\n\n"
                            + "Sign in to your dashboard to view the latest application status.\n\n"
                            + "— EZFINANZ",
                    false
            );
            mailSender.send(mimeMessage);
            log.info("Application review email sent to {}", email);
        } catch (MailException | jakarta.mail.MessagingException | java.io.UnsupportedEncodingException ex) {
            log.error("Failed to send application review email to {}: {}", email, ex.getMessage(), ex);
            throw new ApiException(
                    HttpStatus.BAD_GATEWAY,
                    "EMAIL_SEND_FAILED",
                    "The review was saved, but the applicant notification email could not be sent."
            );
        }
    }

    private static String rootMessage(Throwable ex) {
        Throwable current = ex;
        while (current.getCause() != null && current.getCause() != current) {
            current = current.getCause();
        }
        return current.getMessage() != null ? current.getMessage() : ex.getMessage();
    }
}
