package com.ezfinanz.selfie.repo;

import com.ezfinanz.selfie.domain.SelfieSubmission;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SelfieRepository extends JpaRepository<SelfieSubmission, Long> {

    Optional<SelfieSubmission> findByUser_Id(Long userId);

    boolean existsByUser_Id(Long userId);
}
