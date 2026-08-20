package com.ezfinanz.notify;

import com.ezfinanz.common.ApiException;
import com.twilio.Twilio;
import com.twilio.rest.verify.v2.Service;
import com.twilio.rest.verify.v2.service.Verification;
import com.twilio.rest.verify.v2.service.VerificationCheck;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

@Component
public class TwilioSmsService {

    private static final Logger log = LoggerFactory.getLogger(TwilioSmsService.class);

    private final String accountSid;
    private final String authToken;
    private final String fromNumber;
    private volatile String verifyServiceSid;

    public TwilioSmsService(
            @Value("${twilio.account-sid}") String accountSid,
            @Value("${twilio.auth-token}") String authToken,
            @Value("${twilio.from-number}") String fromNumber,
            @Value("${twilio.verify-service-sid:}") String verifyServiceSid
    ) {
        this.accountSid = accountSid == null ? "" : accountSid.trim();
        this.authToken = authToken == null ? "" : authToken.trim();
        this.fromNumber = fromNumber == null ? "" : fromNumber.trim();
        this.verifyServiceSid = verifyServiceSid == null ? "" : verifyServiceSid.trim();
    }

    public void sendOtp(String phone) {
        ensureConfigured();
        try {
            Twilio.init(accountSid, authToken);
            String serviceSid = resolveVerifyServiceSid();
            log.info("Sending Twilio Verify SMS to {} using trial 2FA template", phone);
            Verification.creator(serviceSid, phone, "sms").create();
            log.info("Twilio Verify accepted SMS to {}", phone);
        } catch (ApiException ex) {
            throw ex;
        } catch (Throwable ex) {
            log.error("Twilio Verify SMS failed to {}: {}", phone, ex.getMessage(), ex);
            throw new ApiException(
                    HttpStatus.BAD_GATEWAY,
                    "SMS_SEND_FAILED",
                    twilioMessage(ex)
            );
        }
    }

    public void checkOtp(String phone, String code) {
        ensureConfigured();
        try {
            Twilio.init(accountSid, authToken);
            String serviceSid = resolveVerifyServiceSid();
            VerificationCheck check = VerificationCheck.creator(serviceSid)
                    .setTo(phone)
                    .setCode(code)
                    .create();
            if (check.getStatus() == null || !"approved".equalsIgnoreCase(check.getStatus())) {
                throw new ApiException(HttpStatus.BAD_REQUEST, "OTP_INVALID", "Invalid or expired verification code.");
            }
        } catch (ApiException ex) {
            throw ex;
        } catch (Throwable ex) {
            log.error("Twilio Verify check failed for {}: {}", phone, ex.getMessage(), ex);
            throw new ApiException(
                    HttpStatus.BAD_REQUEST,
                    "OTP_INVALID",
                    twilioMessage(ex)
            );
        }
    }

    private void ensureConfigured() {
        if (accountSid.isBlank() || authToken.isBlank()) {
            throw new ApiException(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "TWILIO_NOT_CONFIGURED",
                    "Twilio credentials are missing. Set TWILIO_ACCOUNT_SID and TWILIO_AUTH_TOKEN."
            );
        }
    }

    private String resolveVerifyServiceSid() {
        if (verifyServiceSid != null && !verifyServiceSid.isBlank()) {
            return verifyServiceSid;
        }
        synchronized (this) {
            if (verifyServiceSid != null && !verifyServiceSid.isBlank()) {
                return verifyServiceSid;
            }
            Service created = Service.creator("EZFINANZ").create();
            verifyServiceSid = created.getSid();
            log.info("Created Twilio Verify service {}", verifyServiceSid);
            return verifyServiceSid;
        }
    }

    private static String twilioMessage(Throwable ex) {
        Throwable current = ex;
        while (current.getCause() != null && current.getCause() != current) {
            current = current.getCause();
        }
        String message = current.getMessage();
        if (message == null || message.isBlank()) {
            message = ex.getMessage();
        }
        if (message == null || message.isBlank()) {
            return "Could not send the SMS verification code via Twilio.";
        }
        return message;
    }
}
