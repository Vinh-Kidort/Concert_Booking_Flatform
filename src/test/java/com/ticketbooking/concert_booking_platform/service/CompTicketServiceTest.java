package com.ticketbooking.concert_booking_platform.service;

import com.ticketbooking.concert_booking_platform.dto.request.IssueCompTicketRequest;
import com.ticketbooking.concert_booking_platform.entity.Concert;
import com.ticketbooking.concert_booking_platform.entity.Organization;
import com.ticketbooking.concert_booking_platform.entity.TicketCategory;
import com.ticketbooking.concert_booking_platform.exception.CompTicketQuotaExhaustedException;
import com.ticketbooking.concert_booking_platform.exception.ResourceNotFoundException;
import com.ticketbooking.concert_booking_platform.repository.CompTicketRepository;
import com.ticketbooking.concert_booking_platform.repository.TicketCategoryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CompTicketServiceTest {

    @Mock private CompTicketRepository compTicketRepository;
    @Mock private TicketCategoryRepository ticketCategoryRepository;


    @Mock private OrganizationMembershipChecker membershipChecker;

    @InjectMocks private CompTicketService compTicketService;

    private Organization organization;
    private Concert concert;
    private TicketCategory category;

    @BeforeEach
    void setUp() {

        organization = Organization.builder().id(10L).name("Test Org").build();
        concert = Concert.builder().id(1L).organization(organization).build();
        category = TicketCategory.builder()
                .id(10L).concert(concert).compQuantity(5).build();


        lenient().when(membershipChecker.isMember(100L, 10L)).thenReturn(true);
    }

    @Test
    void issueCompTicket_underQuota_succeeds() {
        when(ticketCategoryRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(category));
        when(compTicketRepository.countByTicketCategoryIdAndStatusNot(10L, "REVOKED")).thenReturn(2L);
        when(compTicketRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        IssueCompTicketRequest request = new IssueCompTicketRequest();
        request.setRecipientName("Jane Sponsor");
        request.setRecipientType("SPONSOR");

        var result = compTicketService.issueCompTicket(100L, 10L, request);

        assertThat(result.getStatus()).isEqualTo("ISSUED");
        assertThat(result.getQrCodeToken()).isNotBlank();
    }

    @Test
    void issueCompTicket_atQuota_throwsQuotaExhausted() {
        when(ticketCategoryRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(category));
        when(compTicketRepository.countByTicketCategoryIdAndStatusNot(10L, "REVOKED")).thenReturn(5L); // == compQuantity

        IssueCompTicketRequest request = new IssueCompTicketRequest();
        request.setRecipientName("Jane Sponsor");
        request.setRecipientType("SPONSOR");

        assertThatThrownBy(() -> compTicketService.issueCompTicket(100L, 10L, request))
                .isInstanceOf(CompTicketQuotaExhaustedException.class);
    }

    @Test
    void issueCompTicket_wrongOrganizer_throwsResourceNotFound() {
        when(ticketCategoryRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(category));

        when(membershipChecker.isMember(999L, 10L)).thenReturn(false);

        IssueCompTicketRequest request = new IssueCompTicketRequest();
        request.setRecipientName("Jane Sponsor");
        request.setRecipientType("SPONSOR");

        assertThatThrownBy(() -> compTicketService.issueCompTicket(999L, 10L, request))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void revokeCompTicket_alreadyCheckedIn_throwsIllegalState() {
        var ticket = com.ticketbooking.concert_booking_platform.entity.CompTicket.builder()
                .id(50L).ticketCategoryId(10L).status("CHECKED_IN").build();

        when(compTicketRepository.findById(50L)).thenReturn(Optional.of(ticket));
        when(ticketCategoryRepository.findById(10L)).thenReturn(Optional.of(category));

        assertThatThrownBy(() -> compTicketService.revokeCompTicket(100L, 50L))
                .isInstanceOf(IllegalStateException.class);
    }
}