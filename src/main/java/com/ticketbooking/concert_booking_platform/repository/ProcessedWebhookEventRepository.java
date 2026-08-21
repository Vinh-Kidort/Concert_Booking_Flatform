package com.ticketbooking.concert_booking_platform.repository;

import com.ticketbooking.concert_booking_platform.entity.ProcessedWebhookEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface ProcessedWebhookEventRepository extends JpaRepository<ProcessedWebhookEvent, Long> {
    Optional<ProcessedWebhookEvent> findByProviderAndEventId(String provider, String eventId);
}