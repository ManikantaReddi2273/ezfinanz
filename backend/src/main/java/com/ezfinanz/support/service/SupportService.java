package com.ezfinanz.support.service;

import com.ezfinanz.auth.domain.User;
import com.ezfinanz.auth.repo.UserRepository;
import com.ezfinanz.common.ApiException;
import com.ezfinanz.support.domain.SupportTicket;
import com.ezfinanz.support.dto.SupportTicketRequest;
import com.ezfinanz.support.dto.SupportTicketResponse;
import com.ezfinanz.support.repo.SupportTicketRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class SupportService {

    private final SupportTicketRepository supportTicketRepository;
    private final UserRepository userRepository;

    public SupportService(SupportTicketRepository supportTicketRepository, UserRepository userRepository) {
        this.supportTicketRepository = supportTicketRepository;
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public List<SupportTicketResponse> list(Long userId) {
        return supportTicketRepository.findByUser_IdOrderByCreatedAtDesc(userId).stream()
                .map(SupportTicketResponse::from)
                .toList();
    }

    @Transactional
    public SupportTicketResponse create(Long userId, SupportTicketRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "UNAUTHORIZED", "Not authenticated"));
        SupportTicket row = new SupportTicket();
        row.setUser(user);
        row.setSubject(request.subject().trim());
        row.setMessage(request.message().trim());
        supportTicketRepository.save(row);
        return SupportTicketResponse.from(row);
    }
}
