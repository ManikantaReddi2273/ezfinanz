package com.ezfinanz.auth.repo;

import com.ezfinanz.auth.domain.OtpChallenge;
import com.ezfinanz.auth.domain.OtpChannel;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/**
 * Persistence access for email OTP challenges (active and latest-by-target lookups).
 */
public interface OtpChallengeRepository extends JpaRepository<OtpChallenge, Long> {

    List<OtpChallenge> findByTargetAndChannelAndConsumedAtIsNull(String target, OtpChannel channel);

    Optional<OtpChallenge> findFirstByTargetAndChannelAndConsumedAtIsNullOrderByCreatedAtDesc(
            String target,
            OtpChannel channel
    );
}
