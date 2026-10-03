package com.ticketbooking.concert_booking_platform.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.OffsetDateTime;

@Entity
@Table(name = "seat_proposals")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class SeatProposal {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "zone_id", nullable = false)
    private Long zoneId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "seat_ids", nullable = false, columnDefinition = "text")
    private String seatIds; // "101,102,103"

    @Column(name = "is_split", nullable = false)
    @Builder.Default
    private Boolean isSplit = false;

    @Column(name = "group_count", nullable = false)
    @Builder.Default
    private Integer groupCount = 1;

    @Column(nullable = false, length = 20)
    @Builder.Default
    private String status = "PENDING";

    @Column(name = "expires_at", nullable = false)
    private OffsetDateTime expiresAt;

    @Column(name = "created_at", insertable = false, updatable = false)
    private OffsetDateTime createdAt;
}