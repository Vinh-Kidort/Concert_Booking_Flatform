package com.ticketbooking.concert_booking_platform.service;

import com.ticketbooking.concert_booking_platform.dto.request.ApplyVoucherRequest;
import com.ticketbooking.concert_booking_platform.dto.request.CreateVoucherRequest;
import com.ticketbooking.concert_booking_platform.dto.response.BookingResponse;
import com.ticketbooking.concert_booking_platform.dto.response.VoucherResponse;
import com.ticketbooking.concert_booking_platform.entity.Booking;
import com.ticketbooking.concert_booking_platform.entity.Voucher;
import com.ticketbooking.concert_booking_platform.enums.BookingStatus;
import com.ticketbooking.concert_booking_platform.exception.ResourceNotFoundException;
import com.ticketbooking.concert_booking_platform.exception.VoucherInvalidException;
import com.ticketbooking.concert_booking_platform.repository.BookingRepository;
import com.ticketbooking.concert_booking_platform.repository.VoucherRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class VoucherService {

    private final VoucherRepository voucherRepository;
    private final BookingRepository bookingRepository;

    private static final int MAX_USAGE_PER_USER = 1; // Giới hạn 1 voucher / 1 user

    /**
     * Apply voucher to an existing PENDING booking.
     * Enforces: Global Quota + Per-User Limit + Expiry + Min Order Amount.
     */
    @Transactional
    public BookingResponse applyVoucherToBooking(Long userId, Long bookingId, ApplyVoucherRequest request) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found: " + bookingId));

        // 1. Ownership & Status Validation
        if (!booking.getUser().getId().equals(userId)) {
            throw new ResourceNotFoundException("Booking not found: " + bookingId);
        }

        if (booking.getStatus() != BookingStatus.PENDING) {
            throw new VoucherInvalidException("Voucher can only be applied to PENDING bookings");
        }

        if (booking.getExpiresAt().isBefore(OffsetDateTime.now())) {
            throw new VoucherInvalidException("Booking has expired");
        }

        if (booking.getVoucherCode() != null) {
            throw new VoucherInvalidException("A voucher is already applied to this booking");
        }

        String code = request.getVoucherCode().trim().toUpperCase();

        // 2. Lock Voucher row (SELECT ... FOR UPDATE) to prevent quota race condition
        Voucher voucher = voucherRepository.findByCodeForUpdate(code)
                .orElseThrow(() -> new VoucherInvalidException("Voucher code not found: " + code));

        // 3. Check Global Validity & Quota
        if (!voucher.isValidNow(OffsetDateTime.now())) {
            throw new VoucherInvalidException("Voucher " + code + " is expired or out of stock");
        }

        // 4. Check Minimum Order Value
        if (booking.getTotalAmount().compareTo(voucher.getMinOrderValue()) < 0) {
            throw new VoucherInvalidException(
                    String.format("Order total (%s) does not reach voucher minimum requirement (%s)",
                            booking.getTotalAmount(), voucher.getMinOrderValue()));
        }

        // 5. Check Per-User Usage Limit (Chống 1 user dùng 1 voucher nhiều lần)
        long userUsageCount = bookingRepository.countByUserIdAndVoucherCodeAndStatusNotIn(
                userId, code, List.of(BookingStatus.CANCELLED, BookingStatus.EXPIRED, BookingStatus.FAILED));

        if (userUsageCount >= MAX_USAGE_PER_USER) {
            throw new VoucherInvalidException("You have already used voucher code: " + code);
        }

        // 6. Update Voucher Usage
        voucher.setUsedQuantity(voucher.getUsedQuantity() + 1);
        voucherRepository.save(voucher);

        // 7. Recalculate Booking Amounts
        BigDecimal discountAmount = voucher.getDiscountAmount().min(booking.getTotalAmount());
        BigDecimal finalAmount = booking.getTotalAmount().subtract(discountAmount);

        booking.setVoucherCode(code);
        booking.setDiscountAmount(discountAmount);
        booking.setFinalAmount(finalAmount);

        // 8. Bọc try-catch xung quanh saveAndFlush (Lớp kiểm tra cứng DB - V3 Unique Index)
        try {
            Booking updatedBooking = bookingRepository.saveAndFlush(booking);
            log.info("Voucher {} applied successfully to booking id={}", code, bookingId);
            return BookingResponse.from(updatedBooking);
        } catch (DataIntegrityViolationException e) {
            // Bắt lỗi TOCTOU Race Condition nếu 2 tab cùng bấm áp voucher tại 1 milisecond
            log.warn("Concurrent voucher application detected for user={} code={}", userId, code);
            throw new VoucherInvalidException("You have already used voucher code: " + code);
        }
    }

    /**
     * Admin/Ops endpoint: Create new Voucher campaign
     */
    @Transactional
    public VoucherResponse createVoucher(CreateVoucherRequest request) {
        String code = request.getCode().trim().toUpperCase();

        if (voucherRepository.findByCode(code).isPresent()) {
            throw new VoucherInvalidException("Voucher code already exists: " + code);
        }

        Voucher voucher = Voucher.builder()
                .code(code)
                .discountAmount(request.getDiscountAmount())
                .minOrderValue(request.getMinOrderValue() != null ? request.getMinOrderValue() : BigDecimal.ZERO)
                .totalQuantity(request.getTotalQuantity())
                .usedQuantity(0)
                .startDate(request.getStartDate())
                .endDate(request.getEndDate())
                .build();

        Voucher saved = voucherRepository.save(voucher);
        return VoucherResponse.from(saved);
    }

    /**
     * Admin/Ops endpoint: List all vouchers
     */
    @Transactional(readOnly = true)
    public Page<VoucherResponse> getAllVouchers(Pageable pageable) {
        return voucherRepository.findAll(pageable).map(VoucherResponse::from);
    }
}