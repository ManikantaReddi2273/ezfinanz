package com.ezfinanz.bank.repo;

import com.ezfinanz.bank.domain.BankAccount;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/** Persistence access for customer disbursement bank accounts. */
public interface BankAccountRepository extends JpaRepository<BankAccount, Long> {

    /** Finds the bank account linked to the given user id. */
    Optional<BankAccount> findByUser_Id(Long userId);

    /** Returns whether the user already has a bank account on file. */
    boolean existsByUser_Id(Long userId);
}
