package com.ticketbooking.concert_booking_platform.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Entity
@Table(name = "price_change_audits")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class PriceChangeAudit {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "ticket_category_id", nullable = false)
    private Long ticketCategoryId;

    @Column(name = "changed_by", nullable = false)
    private Long changedBy;

    @Column(name = "old_price", nullable = false, precision = 12, scale = 2)
    private BigDecimal oldPrice;

    @Column(name = "new_price", nullable = false, precision = 12, scale = 2)
    private BigDecimal newPrice;

    @Column(name = "changed_at", insertable = false, updatable = false)
    private OffsetDateTime changedAt;
}