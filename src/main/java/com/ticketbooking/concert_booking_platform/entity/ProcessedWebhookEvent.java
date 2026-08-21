package com.ticketbooking.concert_booking_platform.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.OffsetDateTime;

@Entity
@Table(name = "processed_webhook_events")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ProcessedWebhookEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 50)
    private String provider;

    @Column(name = "event_id", nullable = false)
    private String eventId;

    @Column(name = "event_type", nullable = false, length = 100)
    private String eventType;

    @Column(name = "processed_at", insertable = false, updatable = false)
    private OffsetDateTime processedAt;
}