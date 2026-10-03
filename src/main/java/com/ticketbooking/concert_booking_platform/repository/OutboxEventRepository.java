package com.ticketbooking.concert_booking_platform.repository;

import com.ticketbooking.concert_booking_platform.entity.OutboxEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface OutboxEventRepository extends JpaRepository<OutboxEvent, Long> {
    List<OutboxEvent> findTop100ByStatusOrderByCreatedAtAsc(String status);
}