package com.ezfinanz.eligibility.repo;

import com.ezfinanz.eligibility.domain.EligibilityAssessment;
import com.ezfinanz.eligibility.domain.EligibilityResult;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/** Persistence access for eligibility assessments. */
public interface EligibilityRepository extends JpaRepository<EligibilityAssessment, Long> {

    /** Finds the assessment linked to the given user id. */
    Optional<EligibilityAssessment> findByUser_Id(Long userId);

    /** Returns whether the user already has an eligibility assessment. */
    boolean existsByUser_Id(Long userId);

    /** Returns whether the user's result is one of the given eligibility outcomes. */
    boolean existsByUser_IdAndResultIn(Long userId, java.util.Collection<EligibilityResult> results);
}
