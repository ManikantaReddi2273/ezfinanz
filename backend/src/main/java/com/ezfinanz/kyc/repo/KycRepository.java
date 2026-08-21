package com.ezfinanz.kyc.repo;

import com.ezfinanz.kyc.domain.KycProfile;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/** Persistence access for customer KYC profiles. */
public interface KycRepository extends JpaRepository<KycProfile, Long> {

    /** Finds the KYC profile linked to the given user id. */
    Optional<KycProfile> findByUser_Id(Long userId);

    /** Returns whether the user already has a KYC profile. */
    boolean existsByUser_Id(Long userId);
}
