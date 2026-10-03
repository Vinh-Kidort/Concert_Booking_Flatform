package com.ticketbooking.concert_booking_platform.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ticketbooking.concert_booking_platform.entity.OutboxEvent;
import com.ticketbooking.concert_booking_platform.repository.OutboxEventRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
@RequiredArgsConstructor
public class OutboxEventPublisher {

    private final OutboxEventRepository outboxEventRepository;
    private final ObjectMapper objectMapper;

    /**
     * Writes an event row to the outbox table. Must ALWAYS be called from
     * within the same @Transactional boundary as the business state change
     * it represents (e.g. booking confirmation) — this is the entire point
     * of the Outbox Pattern: the event write and the state change either
     * both commit or both roll back together, so we never end up confirming
     * a booking without a corresponding event, or vice versa.
     */
    public void publish(String aggregateType, Long aggregateId, String eventType, Map<String, Object> payloadData) {
        try {
            String payloadJson = objectMapper.writeValueAsString(payloadData);
            OutboxEvent event = OutboxEvent.builder()
                    .aggregateType(aggregateType)
                    .aggregateId(aggregateId)
                    .eventType(eventType)
                    .payload(payloadJson)
                    .build();
            outboxEventRepository.save(event);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to serialize outbox event payload", e);
        }
    }
}