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

    private static String rootMessage(Throwable ex) {
        Throwable current = ex;
        while (current.getCause() != null && current.getCause() != current) {
            current = current.getCause();
        }
        return current.getMessage() != null ? current.getMessage() : ex.getMessage();
    }
}
