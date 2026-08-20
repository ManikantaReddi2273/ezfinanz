package com.ezfinanz.support.repo;

import com.ezfinanz.support.domain.SupportTicket;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SupportTicketRepository extends JpaRepository<SupportTicket, Long> {

    List<SupportTicket> findByUser_IdOrderByCreatedAtDesc(Long userId);
}
