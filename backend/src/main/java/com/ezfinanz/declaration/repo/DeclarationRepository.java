package com.ezfinanz.declaration.repo;

import com.ezfinanz.declaration.domain.LoanDeclaration;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface DeclarationRepository extends JpaRepository<LoanDeclaration, Long> {

    Optional<LoanDeclaration> findByUser_Id(Long userId);

    boolean existsByUser_IdAndAcceptedIsTrue(Long userId);
}
