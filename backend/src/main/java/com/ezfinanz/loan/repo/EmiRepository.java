package com.ezfinanz.loan.repo;

import com.ezfinanz.loan.domain.EmiSelection;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface EmiRepository extends JpaRepository<EmiSelection, Long> {

    Optional<EmiSelection> findByUser_Id(Long userId);

    boolean existsByUser_Id(Long userId);
}
