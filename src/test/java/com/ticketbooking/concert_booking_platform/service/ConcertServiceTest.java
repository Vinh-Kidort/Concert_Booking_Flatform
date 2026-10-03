package com.ticketbooking.concert_booking_platform.service;

import com.ticketbooking.concert_booking_platform.entity.Concert;
import com.ticketbooking.concert_booking_platform.entity.Organization;
import com.ticketbooking.concert_booking_platform.entity.User;
import com.ticketbooking.concert_booking_platform.enums.ConcertApprovalStatus;
import com.ticketbooking.concert_booking_platform.enums.ConcertStatus;
import com.ticketbooking.concert_booking_platform.exception.InvalidApprovalTransitionException;
import com.ticketbooking.concert_booking_platform.exception.ResourceNotFoundException;
import com.ticketbooking.concert_booking_platform.repository.*;
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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ConcertServiceTest {

    @Mock private ConcertRepository concertRepository;
    @Mock private TicketCategoryRepository ticketCategoryRepository;
    @Mock private BookingRepository bookingRepository;
    @Mock private UserRepository userRepository;
    @Mock private OrganizationRepository organizationRepository;
    @Mock private OrganizationMemberRepository organizationMemberRepository;

    // 👉 THÊM MOCK MEMBERSHIP CHECKER:
    @Mock private OrganizationMembershipChecker membershipChecker;

    @InjectMocks private ConcertService concertService;

    private Concert concert;
    private User organizer;
    private Organization organization;

    @BeforeEach
    void setUp() {
        organizer = User.builder().id(100L).email("org@test.com").build();
        organization = Organization.builder().id(10L).name("Test Org").build();

        concert = Concert.builder()
                .id(1L)
                .organization(organization) // 👉 GẮN ORGANIZATION VÀO CONCERT
                .approvalStatus(ConcertApprovalStatus.PENDING_REVIEW)
                .status(ConcertStatus.UPCOMING)
                .build();

        // Mặc định User 100L là thành viên của Organization 10L
        lenient().when(membershipChecker.isMember(eq(100L), eq(10L))).thenReturn(true);
        // User 999L không thuộc Organization 10L
        lenient().when(membershipChecker.isMember(eq(999L), eq(10L))).thenReturn(false);
    }

    @Test
    void approveConcert_correctOrganizer_transitionsToApproved() {
        when(concertRepository.findById(1L)).thenReturn(Optional.of(concert));
        when(userRepository.getReferenceById(100L)).thenReturn(organizer);
        when(concertRepository.save(any(Concert.class))).thenAnswer(inv -> inv.getArgument(0));

        Concert result = concertService.approveConcert(100L, 1L);

        assertThat(result.getApprovalStatus()).isEqualTo(ConcertApprovalStatus.APPROVED);
    }

    @Test
    void approveConcert_wrongOrganizer_throwsResourceNotFound() {
        when(concertRepository.findById(1L)).thenReturn(Optional.of(concert));

        assertThatThrownBy(() -> concertService.approveConcert(999L, 1L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void approveConcert_wrongStatus_throwsInvalidTransition() {
        concert.setApprovalStatus(ConcertApprovalStatus.DRAFT);
        when(concertRepository.findById(1L)).thenReturn(Optional.of(concert));

        assertThatThrownBy(() -> concertService.approveConcert(100L, 1L))
                .isInstanceOf(InvalidApprovalTransitionException.class);
    }

    @Test
    void rejectConcert_correctOrganizer_setsReasonAndStatus() {
        when(concertRepository.findById(1L)).thenReturn(Optional.of(concert));
        when(userRepository.getReferenceById(100L)).thenReturn(organizer);
        when(concertRepository.save(any(Concert.class))).thenAnswer(inv -> inv.getArgument(0));

        Concert result = concertService.rejectConcert(100L, 1L, "Wrong seat pricing");

        assertThat(result.getApprovalStatus()).isEqualTo(ConcertApprovalStatus.REJECTED);
        assertThat(result.getRejectionReason()).isEqualTo("Wrong seat pricing");
    }

    @Test
    void publishConcert_notApproved_throwsInvalidTransition() {
        concert.setApprovalStatus(ConcertApprovalStatus.PENDING_REVIEW);
        when(concertRepository.findById(1L)).thenReturn(Optional.of(concert));

        assertThatThrownBy(() -> concertService.publishConcert(1L))
                .isInstanceOf(InvalidApprovalTransitionException.class);
    }

    @Test
    void updateConcert_whenOnSale_throwsIllegalState() {
        concert.setStatus(ConcertStatus.ON_SALE);
        when(concertRepository.findById(1L)).thenReturn(Optional.of(concert));

        assertThatThrownBy(() -> concertService.updateConcert(1L,
                new com.ticketbooking.concert_booking_platform.dto.request.UpdateConcertRequest()))
                .isInstanceOf(IllegalStateException.class);
    }
}