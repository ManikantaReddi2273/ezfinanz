package com.ezfinanz.auth.service;

import com.ezfinanz.auth.domain.OtpChallenge;
import com.ezfinanz.auth.domain.OtpChannel;
import com.ezfinanz.auth.domain.OtpPurpose;
import com.ezfinanz.auth.repo.OtpChallengeRepository;
import com.ezfinanz.common.ApiException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Service
public class OtpService {

    private static final SecureRandom RANDOM = new SecureRandom();

    private final OtpChallengeRepository otpChallengeRepository;
    private final PasswordEncoder passwordEncoder;
    private final int ttlMinutes;

    public OtpService(
            OtpChallengeRepository otpChallengeRepository,
            PasswordEncoder passwordEncoder,
            @Value("${app.otp.ttl-minutes}") int ttlMinutes
    ) {
        this.otpChallengeRepository = otpChallengeRepository;
        this.passwordEncoder = passwordEncoder;
        this.ttlMinutes = ttlMinutes;
    }

    public int getTtlMinutes() {
        return ttlMinutes;
    }

    @Transactional
    public String issue(String target, OtpChannel channel, OtpPurpose purpose) {
        List<OtpChallenge> previous = otpChallengeRepository.findByTargetAndChannelAndConsumedAtIsNull(target, channel);
        Instant now = Instant.now();
        for (OtpChallenge challenge : previous) {
            challenge.setConsumedAt(now);
        }
        otpChallengeRepository.saveAll(previous);

        String code = String.format("%06d", RANDOM.nextInt(1_000_000));
        OtpChallenge challenge = new OtpChallenge();
        challenge.setChannel(channel);
        challenge.setTarget(target);
        challenge.setCodeHash(passwordEncoder.encode(code));
        challenge.setPurpose(purpose);
        challenge.setExpiresAt(now.plus(ttlMinutes, ChronoUnit.MINUTES));
        otpChallengeRepository.save(challenge);
        return code;
    }

    @Transactional
    public void verify(String target, OtpChannel channel, String rawCode) {
        OtpChallenge challenge = otpChallengeRepository
                .findFirstByTargetAndChannelAndConsumedAtIsNullOrderByCreatedAtDesc(target, channel)
                .orElseThrow(() -> new ApiException(
                        HttpStatus.BAD_REQUEST,
                        "OTP_NOT_FOUND",
                        "No active verification code. Request a new one."
                ));
        if (challenge.isExpired()) {
            challenge.setConsumedAt(Instant.now());
            otpChallengeRepository.save(challenge);
            throw new ApiException(HttpStatus.BAD_REQUEST, "OTP_EXPIRED", "Verification code has expired.");
        }
        if (!passwordEncoder.matches(rawCode, challenge.getCodeHash())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "OTP_INVALID", "Invalid verification code.");
        }
        challenge.setConsumedAt(Instant.now());
        otpChallengeRepository.save(challenge);
    }
}
