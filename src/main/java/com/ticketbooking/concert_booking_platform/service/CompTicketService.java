package com.ticketbooking.concert_booking_platform.service;

import com.ticketbooking.concert_booking_platform.dto.request.IssueCompTicketRequest;
import com.ticketbooking.concert_booking_platform.dto.response.SeatAllocationReportResponse;
import com.ticketbooking.concert_booking_platform.entity.CompTicket;
import com.ticketbooking.concert_booking_platform.entity.Concert;
import com.ticketbooking.concert_booking_platform.entity.TicketCategory;
import com.ticketbooking.concert_booking_platform.exception.CompTicketQuotaExhaustedException;
import com.ticketbooking.concert_booking_platform.exception.ResourceNotFoundException;
import com.ticketbooking.concert_booking_platform.repository.CompTicketRepository;
import com.ticketbooking.concert_booking_platform.repository.TicketCategoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class CompTicketService {

    private final CompTicketRepository compTicketRepository;
    private final TicketCategoryRepository ticketCategoryRepository;

    // 👉 ĐƯA BIẾN NÀY LÊN ĐẦU CLASS ĐỂ LOMBOK GENERATE CONSTRUCTOR CHUẨN XÁC:
    private final OrganizationMembershipChecker membershipChecker;

    @Transactional
    public CompTicket issueCompTicket(Long organizerId, Long ticketCategoryId, IssueCompTicketRequest request) {
        TicketCategory category = ticketCategoryRepository.findByIdForUpdate(ticketCategoryId)
                .orElseThrow(() -> new ResourceNotFoundException("Ticket category not found: " + ticketCategoryId));

        // Đổi sang gọi verifyOrganizationMembership
        verifyOrganizationMembership(organizerId, category);

        long issuedCount = compTicketRepository.countByTicketCategoryIdAndStatusNot(ticketCategoryId, "REVOKED");
        if (issuedCount >= category.getCompQuantity()) {
            throw new CompTicketQuotaExhaustedException(String.format(
                    "Comp ticket quota exhausted: %d/%d already issued for this category",
                    issuedCount, category.getCompQuantity()));
        }

        CompTicket ticket = CompTicket.builder()
                .ticketCategoryId(ticketCategoryId)
                .issuedBy(organizerId)
                .recipientName(request.getRecipientName())
                .recipientEmail(request.getRecipientEmail())
                .recipientType(request.getRecipientType())
                .qrCodeToken(UUID.randomUUID().toString())
                .status("ISSUED")
                .build();

        CompTicket saved = compTicketRepository.save(ticket);
        log.info("Organizer {} issued comp ticket {} for category {} to {}",
                organizerId, saved.getId(), ticketCategoryId, request.getRecipientName());

        return saved;
    }

    @Transactional
    public void revokeCompTicket(Long organizerId, Long compTicketId) {
        CompTicket ticket = compTicketRepository.findById(compTicketId)
                .orElseThrow(() -> new ResourceNotFoundException("Comp ticket not found: " + compTicketId));

        TicketCategory category = ticketCategoryRepository.findById(ticket.getTicketCategoryId())
                .orElseThrow(() -> new ResourceNotFoundException("Ticket category not found"));

        // Đổi sang gọi verifyOrganizationMembership
        verifyOrganizationMembership(organizerId, category);

        if ("CHECKED_IN".equals(ticket.getStatus())) {
            throw new IllegalStateException("Cannot revoke a ticket that has already been checked in");
        }

        ticket.setStatus("REVOKED");
        compTicketRepository.save(ticket);
    }

    @Transactional(readOnly = true)
    public Page<CompTicket> listByCategory(Long organizerId, Long ticketCategoryId, Pageable pageable) {
        TicketCategory category = ticketCategoryRepository.findById(ticketCategoryId)
                .orElseThrow(() -> new ResourceNotFoundException("Ticket category not found"));

        // Đổi sang gọi verifyOrganizationMembership
        verifyOrganizationMembership(organizerId, category);

        return compTicketRepository.findByTicketCategoryId(ticketCategoryId, pageable);
    }

    @Transactional(readOnly = true)
    public SeatAllocationReportResponse getSeatAllocationReport(Long organizerId, Long ticketCategoryId) {
        TicketCategory category = ticketCategoryRepository.findById(ticketCategoryId)
                .orElseThrow(() -> new ResourceNotFoundException("Ticket category not found"));

        // Đổi sang gọi verifyOrganizationMembership
        verifyOrganizationMembership(organizerId, category);

        long compIssued = compTicketRepository.countByTicketCategoryIdAndStatusNot(ticketCategoryId, "REVOKED");
        int compRemaining = category.getCompQuantity() - (int) compIssued;
        int soldQuantity = category.getTotalQuantity() - category.getCompQuantity() - category.getAvailableQuantity();

        return SeatAllocationReportResponse.builder()
                .ticketCategoryId(category.getId())
                .categoryName(category.getName())
                .totalQuantity(category.getTotalQuantity())
                .soldQuantity(soldQuantity)
                .compIssuedQuantity((int) compIssued)
                .compRemainingQuantity(compRemaining)
                .publicAvailableQuantity(category.getAvailableQuantity())
                .unallocatedQuantity(category.getAvailableQuantity() + compRemaining)
                .build();
    }


    private void verifyOrganizationMembership(Long userId, TicketCategory category) {
        Concert concert = category.getConcert();
        if (concert == null || concert.getOrganization() == null
                || !membershipChecker.isMember(userId, concert.getOrganization().getId())) {
            throw new ResourceNotFoundException("Ticket category not found: " + category.getId());
        }
    }
}