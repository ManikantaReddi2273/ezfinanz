package com.ezfinanz.loan.repo;

import com.ezfinanz.loan.domain.EmiSelection;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/** Persistence access for customer EMI selections. */
public interface EmiRepository extends JpaRepository<EmiSelection, Long> {

    /** Finds the EMI selection linked to the given user id. */
    Optional<EmiSelection> findByUser_Id(Long userId);

    /** Returns whether the user already confirmed EMI terms. */
    boolean existsByUser_Id(Long userId);
}
