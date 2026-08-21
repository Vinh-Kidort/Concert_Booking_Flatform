package com.ticketbooking.concert_booking_platform.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

/**
 * Concurrency note: this is the "hot row" during flash sale — many
 * concurrent requests contend for the same ticket_category row. The
 * concurrency-control strategy is PESSIMISTIC LOCK (SELECT ... FOR UPDATE),
 * applied via {@code TicketCategoryRepository#findByIdForUpdate}. The
 * {@code version} column is kept only for audit/reference, it is NOT used
 * as an @Version optimistic-lock field on purpose — see ARCHITECTURE.md.
 */
@Entity
@Table(name = "ticket_categories")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TicketCategory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "concert_id", nullable = false)
    private Concert concert;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal price;

    @Column(name = "total_quantity", nullable = false)
    private Integer totalQuantity;

    @Column(name = "available_quantity", nullable = false)
    private Integer availableQuantity;

    // Reference-only counter, NOT a JPA @Version field. Concurrency safety
    // for this entity is handled via pessimistic locking in the service
    // layer, not Hibernate optimistic locking.
    @Column(nullable = false)
    @Builder.Default
    private Long version = 0L;

    @Column(name = "created_at", insertable = false, updatable = false)
    private OffsetDateTime createdAt;
}