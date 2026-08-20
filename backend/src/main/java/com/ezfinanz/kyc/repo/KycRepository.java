package com.ezfinanz.kyc.repo;

import com.ezfinanz.kyc.domain.KycProfile;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface KycRepository extends JpaRepository<KycProfile, Long> {

    Optional<KycProfile> findByUser_Id(Long userId);

    boolean existsByUser_Id(Long userId);
}
