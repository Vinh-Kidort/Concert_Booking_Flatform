package com.ticketbooking.concert_booking_platform.service;

import com.ticketbooking.concert_booking_platform.entity.Concert;
import com.ticketbooking.concert_booking_platform.entity.PriceChangeAudit;
import com.ticketbooking.concert_booking_platform.entity.TicketCategory;
import com.ticketbooking.concert_booking_platform.exception.DiscountNotAllowedException;
import com.ticketbooking.concert_booking_platform.exception.ResourceNotFoundException;
import com.ticketbooking.concert_booking_platform.repository.PriceChangeAuditRepository;
import com.ticketbooking.concert_booking_platform.repository.TicketCategoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class PricingService {

    private static final int DISCOUNT_WINDOW_DAYS = 3;
    private static final BigDecimal MIN_PRICE_RATIO = new BigDecimal("0.5"); // giá sàn = 50% original

    private final TicketCategoryRepository ticketCategoryRepository;
    private final PriceChangeAuditRepository priceChangeAuditRepository;

    // 👉 ĐÃ ĐƯA BIẾN NÀY LÊN ĐẦU CLASS ĐỒNG BỘ VỚI CompTicketService:
    private final OrganizationMembershipChecker membershipChecker;

    /**
     * Organizer-only discount action. Concurrency note: locks the same
     * ticket_categories row via findByIdForUpdate() used by
     * BookingTransactionExecutor when reserving stock. Because both paths
     * lock through the same row, a discount applied here and a concurrent
     * booking reservation are automatically serialized by Postgres — a
     * booking mid-flight will always see either the price before or after
     * the discount, never a partially-applied state.
     */
    @CacheEvict(value = {"concerts", "concertDetail"}, allEntries = true)
    @Transactional
    public TicketCategory applyDiscount(Long organizerId, Long ticketCategoryId, BigDecimal newPrice) {
        TicketCategory category = ticketCategoryRepository.findByIdForUpdate(ticketCategoryId)
                .orElseThrow(() -> new ResourceNotFoundException("Ticket category not found: " + ticketCategoryId));

        // Kiểm tra quyền thuộc tổ chức
        verifyOrganizationMembership(organizerId, category);

        Concert concert = category.getConcert();
        OffsetDateTime discountWindowStart = concert.getEventDate().minusDays(DISCOUNT_WINDOW_DAYS);
        if (OffsetDateTime.now().isBefore(discountWindowStart)) {
            throw new DiscountNotAllowedException(String.format(
                    "Discounts can only be applied within %d days before the event date (%s)",
                    DISCOUNT_WINDOW_DAYS, concert.getEventDate()));
        }

        if (newPrice.compareTo(category.getPrice()) >= 0) {
            throw new DiscountNotAllowedException(
                    "New price must be lower than the current price (" + category.getPrice() + ")");
        }

        BigDecimal minAllowedPrice = category.getOriginalPrice().multiply(MIN_PRICE_RATIO);
        if (newPrice.compareTo(minAllowedPrice) < 0) {
            throw new DiscountNotAllowedException(
                    "Cannot discount below 50% of the original price (minimum allowed: " + minAllowedPrice + ")");
        }

        BigDecimal oldPrice = category.getPrice();

        category.setPrice(newPrice);
        category.setDiscountedPrice(newPrice);
        category.setDiscountAppliedAt(OffsetDateTime.now());
        TicketCategory saved = ticketCategoryRepository.save(category);

        priceChangeAuditRepository.save(PriceChangeAudit.builder()
                .ticketCategoryId(ticketCategoryId)
                .changedBy(organizerId)
                .oldPrice(oldPrice)
                .newPrice(newPrice)
                .build());

        log.info("Organizer {} discounted ticket category {} from {} to {}",
                organizerId, ticketCategoryId, oldPrice, newPrice);

        return saved;
    }

    @Transactional(readOnly = true)
    public Page<PriceChangeAudit> getPriceHistory(Long organizerId, Long ticketCategoryId, Pageable pageable) {
        TicketCategory category = ticketCategoryRepository.findById(ticketCategoryId)
                .orElseThrow(() -> new ResourceNotFoundException("Ticket category not found: " + ticketCategoryId));

        // Kiểm tra quyền thuộc tổ chức
        verifyOrganizationMembership(organizerId, category);

        return priceChangeAuditRepository.findByTicketCategoryIdOrderByChangedAtDesc(ticketCategoryId, pageable);
    }

    // 👉 HÀM VERIFY ĐƯỢC CHUẨN HÓA GIỐNG 100% VỚI CompTicketService:
    private void verifyOrganizationMembership(Long userId, TicketCategory category) {
        Concert concert = category.getConcert();
        if (concert == null || concert.getOrganization() == null
                || !membershipChecker.isMember(userId, concert.getOrganization().getId())) {
            throw new ResourceNotFoundException("Ticket category not found: " + category.getId());
        }
    }
}