package com.ticketbooking.concert_booking_platform.scheduler;

import com.ticketbooking.concert_booking_platform.entity.Booking;
import com.ticketbooking.concert_booking_platform.enums.BookingStatus;
import com.ticketbooking.concert_booking_platform.exception.InvalidBookingStatusTransitionException;
import com.ticketbooking.concert_booking_platform.repository.BookingRepository;
import com.ticketbooking.concert_booking_platform.service.BookingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Slice;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;

@Component
@RequiredArgsConstructor
@Slf4j
public class BookingExpiryScheduler {

    private final BookingRepository bookingRepository;
    private final BookingService bookingService;

    private static final int BATCH_SIZE = 100; // Mỗi lần chỉ xử lý 100 đơn để tránh OOM

    @Scheduled(fixedDelay = 30_000)
    public void releaseExpiredBookings() {
        OffsetDateTime now = OffsetDateTime.now();

        // Quét từng Batch 100 đơn
        Slice<Booking> expiredBatch = bookingRepository.findByStatusInAndExpiresAtBefore(
                BookingStatus.ACTIVE_HOLD_STATUSES, now, PageRequest.of(0, BATCH_SIZE));

        if (expiredBatch.isEmpty()) {
            return;
        }

        int successCount = 0;
        for (Booking booking : expiredBatch) {
            try {
                bookingService.updateStatus(booking.getId(), BookingStatus.EXPIRED,
                        null, "Auto-expired: hold time exceeded");
                successCount++;
            } catch (InvalidBookingStatusTransitionException e) {
                // RACE CONDITION KỲ VỌNG: User vừa kịp thanh toán trước 1ms -> Trạng thái đã đổi sang CONFIRMED/PAID
                log.info("Booking id={} was updated by another process before auto-expiry: {}",
                        booking.getId(), e.getMessage());
            } catch (Exception e) {
                // Lỗi hệ thống thực sự (DB down, Lock Timeout...) mới log ERROR
                log.error("Unexpected error auto-expiring booking id={}", booking.getId(), e);
            }
        }

        if (successCount > 0) {
            log.info("Auto-expired {} bookings in this batch", successCount);
        }
    }
}