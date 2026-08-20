package com.ezfinanz.bank.repo;

import com.ezfinanz.bank.domain.BankAccount;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface BankAccountRepository extends JpaRepository<BankAccount, Long> {

    Optional<BankAccount> findByUser_Id(Long userId);

    boolean existsByUser_Id(Long userId);
}
