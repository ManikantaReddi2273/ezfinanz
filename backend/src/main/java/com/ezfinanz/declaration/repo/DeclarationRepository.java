package com.ezfinanz.declaration.repo;

import com.ezfinanz.declaration.domain.LoanDeclaration;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/** Persistence access for loan declaration acceptances. */
public interface DeclarationRepository extends JpaRepository<LoanDeclaration, Long> {

    /** Finds the declaration linked to the given user id. */
    Optional<LoanDeclaration> findByUser_Id(Long userId);

    /** Returns whether the user has accepted the declaration. */
    boolean existsByUser_IdAndAcceptedIsTrue(Long userId);
}
