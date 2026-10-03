package com.ticketbooking.concert_booking_platform.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.OffsetDateTime;

@Entity
@Table(name = "organization_members")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class OrganizationMember {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "organization_id", nullable = false)
    private Long organizationId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "member_role", nullable = false, length = 20)
    private String memberRole; // OWNER, STAFF

    @Column(name = "added_at", insertable = false, updatable = false)
    private OffsetDateTime addedAt;
}