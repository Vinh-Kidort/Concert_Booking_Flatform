package com.ticketbooking.concert_booking_platform.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.OffsetDateTime;

@Entity
@Table(name = "comp_tickets")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class CompTicket {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "ticket_category_id", nullable = false)
    private Long ticketCategoryId;

    @Column(name = "issued_by", nullable = false)
    private Long issuedBy;

    @Column(name = "recipient_name", nullable = false)
    private String recipientName;

    @Column(name = "recipient_email")
    private String recipientEmail;

    @Column(name = "recipient_type", nullable = false, length = 30)
    private String recipientType;

    @Column(name = "qr_code_token", nullable = false, unique = true)
    private String qrCodeToken;

    @Column(nullable = false, length = 20)
    @Builder.Default
    private String status = "ISSUED";

    @Column(name = "issued_at", insertable = false, updatable = false)
    private OffsetDateTime issuedAt;

    @Column(name = "checked_in_at")
    private OffsetDateTime checkedInAt;
}