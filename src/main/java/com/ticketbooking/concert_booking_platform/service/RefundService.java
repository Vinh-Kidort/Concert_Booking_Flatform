package com.ticketbooking.concert_booking_platform.service;

import com.stripe.exception.StripeException;
import com.stripe.model.PaymentIntent;
import com.stripe.model.Refund;
import com.stripe.param.RefundCreateParams;
import com.ticketbooking.concert_booking_platform.dto.response.RefundResponse;
import com.ticketbooking.concert_booking_platform.entity.Booking;
import com.ticketbooking.concert_booking_platform.enums.BookingStatus;
import com.ticketbooking.concert_booking_platform.exception.RefundNotAllowedException;
import com.ticketbooking.concert_booking_platform.exception.ResourceNotFoundException;
import com.ticketbooking.concert_booking_platform.repository.BookingRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;

@Service
@RequiredArgsConstructor
@Slf4j
public class RefundService {

    private final BookingRepository bookingRepository;
    private final RefundPolicy refundPolicy;

    /**
     * Requests a refund for a CONFIRMED booking. Splits into two steps by
     * design: (1) validate + compute amount + call Stripe here, (2) the
     * actual booking status change to CANCELLED happens in the
     * charge.refunded webhook handler, not here — mirrors the same
     * pattern already used for payment confirmation, where Stripe's
     * webhook is the single source of truth for "did the money actually
     * move", not our own optimistic assumption right after calling their API.
     */
    @Transactional
    public RefundResponse requestRefund(Long userId, Long bookingId) throws StripeException {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found: " + bookingId));

        if (!booking.getUser().getId().equals(userId)) {
            throw new ResourceNotFoundException("Booking not found: " + bookingId);
        }
        if (booking.getStatus() != BookingStatus.CONFIRMED) {
            throw new RefundNotAllowedException(
                    "Only CONFIRMED bookings can be refunded, current status: " + booking.getStatus());
        }
        if (booking.getStripePaymentIntentId() == null) {
            throw new RefundNotAllowedException("This booking has no associated payment to refund");
        }
        if (booking.getRefundRequestedAt() != null) {
            throw new RefundNotAllowedException("A refund has already been requested for this booking");
        }

        // 1. Lấy thông tin thanh toán THỰC TẾ từ Stripe (Tránh lỗi lệch đơn vị tiền tệ)
        PaymentIntent paymentIntent = PaymentIntent.retrieve(booking.getStripePaymentIntentId());
        long chargedAmountInCents = paymentIntent.getAmount(); // Ví dụ: 10000 cents ($100.00)

        // 2. Tính tỷ lệ hoàn tiền theo ngày (ví dụ 70% = 0.70)
        long daysUntilEvent = ChronoUnit.DAYS.between(
                OffsetDateTime.now().toLocalDate(),
                booking.getConcert().getEventDate().toLocalDate());
        BigDecimal refundRate = refundPolicy.resolveRefundRate(daysUntilEvent);

        // 3. Tính số tiền cents cần hoàn trên Stripe (Ví dụ: 10000 * 0.7 = 7000 cents = $70.00)
        long refundAmountInCents = BigDecimal.valueOf(chargedAmountInCents)
                .multiply(refundRate)
                .longValue();

        // 4. Tạo yêu cầu hoàn tiền trên Stripe
        RefundCreateParams params = RefundCreateParams.builder()
                .setPaymentIntent(booking.getStripePaymentIntentId())
                .setAmount(refundAmountInCents)
                .putMetadata("bookingId", String.valueOf(booking.getId()))
                .build();

        Refund refund = Refund.create(params);

        // 5. Cập nhật thông tin vào Database
        BigDecimal refundAmountDisplay = BigDecimal.valueOf(refundAmountInCents).divide(BigDecimal.valueOf(100));
        booking.setRefundAmount(refundAmountDisplay);
        booking.setRefundRequestedAt(OffsetDateTime.now());
        booking.setStripeRefundId(refund.getId());
        bookingRepository.save(booking);

        log.info("Refund requested for booking {}: amountInCents={}, stripeRefundId={}",
                bookingId, refundAmountInCents, refund.getId());

        return RefundResponse.builder()
                .bookingId(booking.getId())
                .refundAmount(refundAmountDisplay)
                .stripeRefundId(refund.getId())
                .status("PENDING")
                .build();
    }
}