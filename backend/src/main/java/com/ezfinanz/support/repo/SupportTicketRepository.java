package com.ezfinanz.support.repo;

import com.ezfinanz.support.domain.SupportTicket;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * JPA access for human support tickets (Contact Support), not chatbot messages.
 */
public interface SupportTicketRepository extends JpaRepository<SupportTicket, Long> {

    /** Returns a customer's tickets ordered by creation time, newest first. */
    List<SupportTicket> findByUser_IdOrderByCreatedAtDesc(Long userId);
}
