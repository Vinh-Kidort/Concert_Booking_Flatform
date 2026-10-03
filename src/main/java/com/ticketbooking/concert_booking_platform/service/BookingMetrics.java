package com.ticketbooking.concert_booking_platform.service;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.stereotype.Component;

@Component
public class BookingMetrics {

    private final Counter bookingSuccessCounter;
    private final Counter bookingFailedInsufficientStockCounter;
    private final Timer bookingCreationTimer;

    public BookingMetrics(MeterRegistry registry) {
        this.bookingSuccessCounter = Counter.builder("booking.creation.success")
                .description("Number of successfully created bookings")
                .register(registry);
        this.bookingFailedInsufficientStockCounter = Counter.builder("booking.creation.failed.insufficient_stock")
                .description("Number of booking attempts rejected due to insufficient ticket stock")
                .register(registry);
        this.bookingCreationTimer = Timer.builder("booking.creation.duration")
                .description("Time taken to create a booking, including lock wait time")
                .publishPercentileHistogram()
                .register(registry);
    }

    public void recordSuccess() {
        bookingSuccessCounter.increment();
    }

    public void recordInsufficientStock() {
        bookingFailedInsufficientStockCounter.increment();
    }

    public Timer.Sample startTimer() {
        return Timer.start();
    }

    public void stopTimer(Timer.Sample sample) {
        // Phòng thủ: Kiểm tra khác null để Mockito Unit Test chạy an toàn
        if (sample != null) {
            sample.stop(bookingCreationTimer);
        }
    }
}