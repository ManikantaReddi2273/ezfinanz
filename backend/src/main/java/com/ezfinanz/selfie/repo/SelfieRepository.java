package com.ezfinanz.selfie.repo;

import com.ezfinanz.selfie.domain.SelfieSubmission;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/** Persistence access for customer selfie submissions. */
public interface SelfieRepository extends JpaRepository<SelfieSubmission, Long> {

    /** Finds the selfie submission linked to the given user id. */
    Optional<SelfieSubmission> findByUser_Id(Long userId);

    /** Returns whether the user already has a selfie on file. */
    boolean existsByUser_Id(Long userId);
}
