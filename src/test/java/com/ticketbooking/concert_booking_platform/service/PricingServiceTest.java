package com.ticketbooking.concert_booking_platform.service;

import com.ticketbooking.concert_booking_platform.entity.Concert;
import com.ticketbooking.concert_booking_platform.entity.Organization;
import com.ticketbooking.concert_booking_platform.entity.TicketCategory;
import com.ticketbooking.concert_booking_platform.entity.User;
import com.ticketbooking.concert_booking_platform.exception.DiscountNotAllowedException;
import com.ticketbooking.concert_booking_platform.exception.ResourceNotFoundException;
import com.ticketbooking.concert_booking_platform.repository.PriceChangeAuditRepository;
import com.ticketbooking.concert_booking_platform.repository.TicketCategoryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PricingServiceTest {

    @Mock private TicketCategoryRepository ticketCategoryRepository;
    @Mock private PriceChangeAuditRepository priceChangeAuditRepository;
    @Mock private OrganizationMembershipChecker membershipChecker;

    @InjectMocks private PricingService pricingService;

    private User organizer;
    private Concert concert;
    private TicketCategory category;

    @BeforeEach
    void setUp() {
        organizer = User.builder().id(100L).build();
        Organization org = Organization.builder().id(10L).build();

        concert = Concert.builder()
                .id(1L)
                .organization(org)
                .eventDate(OffsetDateTime.now().plusDays(2))
                .build();

        category = TicketCategory.builder()
                .id(10L)
                .concert(concert)
                .price(new BigDecimal("1000.00"))
                .originalPrice(new BigDecimal("1000.00"))
                .build();

        // Mặc định cho phép user 100 thuộc org 10 để các test case tính giá chạy đúng
        lenient().when(membershipChecker.isMember(100L, 10L)).thenReturn(true);
    }

    @Test
    void applyDiscount_validRequest_updatesPriceAndWritesAudit() {
        when(ticketCategoryRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(category));
        when(ticketCategoryRepository.save(any(TicketCategory.class))).thenAnswer(inv -> inv.getArgument(0));

        TicketCategory result = pricingService.applyDiscount(100L, 10L, new BigDecimal("600.00"));

        assertThat(result.getPrice()).isEqualByComparingTo("600.00");
        assertThat(result.getDiscountedPrice()).isEqualByComparingTo("600.00");
        verify(priceChangeAuditRepository).save(any());
    }

    @Test
    void applyDiscount_wrongOrganizer_throwsResourceNotFound() {
        when(ticketCategoryRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(category));

        assertThatThrownBy(() -> pricingService.applyDiscount(999L, 10L, new BigDecimal("600.00")))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void applyDiscount_outsideDiscountWindow_throwsDiscountNotAllowed() {
        concert.setEventDate(OffsetDateTime.now().plusDays(10)); // ngoài window 3 ngày
        when(ticketCategoryRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(category));

        assertThatThrownBy(() -> pricingService.applyDiscount(100L, 10L, new BigDecimal("600.00")))
                .isInstanceOf(DiscountNotAllowedException.class)
                .hasMessageContaining("3 days");
    }

    @Test
    void applyDiscount_belowFiftyPercentFloor_throwsDiscountNotAllowed() {
        when(ticketCategoryRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(category));

        // original 1000, floor = 500, thử giảm còn 400 -> phải bị từ chối
        assertThatThrownBy(() -> pricingService.applyDiscount(100L, 10L, new BigDecimal("400.00")))
                .isInstanceOf(DiscountNotAllowedException.class)
                .hasMessageContaining("50%");
    }

    @Test
    void applyDiscount_higherThanCurrentPrice_throwsDiscountNotAllowed() {
        when(ticketCategoryRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(category));

        assertThatThrownBy(() -> pricingService.applyDiscount(100L, 10L, new BigDecimal("1200.00")))
                .isInstanceOf(DiscountNotAllowedException.class)
                .hasMessageContaining("lower than");
    }

    @Test
    void applyDiscount_exactlyAtFiftyPercentFloor_succeeds() {
        when(ticketCategoryRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(category));
        when(ticketCategoryRepository.save(any(TicketCategory.class))).thenAnswer(inv -> inv.getArgument(0));

        // đúng biên 50% (500.00) - phải PASS, không throw
        TicketCategory result = pricingService.applyDiscount(100L, 10L, new BigDecimal("500.00"));

        assertThat(result.getPrice()).isEqualByComparingTo("500.00");
    }
}