package com.ezfinanz.eligibility.repo;

import com.ezfinanz.eligibility.domain.EligibilityAssessment;
import com.ezfinanz.eligibility.domain.EligibilityResult;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface EligibilityRepository extends JpaRepository<EligibilityAssessment, Long> {

    Optional<EligibilityAssessment> findByUser_Id(Long userId);

    boolean existsByUser_Id(Long userId);

    boolean existsByUser_IdAndResultIn(Long userId, java.util.Collection<EligibilityResult> results);
}
