package com.ticketbooking.concert_booking_platform.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "seat_rows")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class SeatRow {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "zone_id", nullable = false)
    private SeatZone zone;

    @Column(name = "row_label", nullable = false, length = 10)
    private String rowLabel;

    @Column(name = "row_priority", nullable = false)
    private Integer rowPriority;

    @Column(name = "seat_count", nullable = false)
    private Integer seatCount;
}