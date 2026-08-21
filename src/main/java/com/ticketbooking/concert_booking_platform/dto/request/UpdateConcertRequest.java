package com.ticketbooking.concert_booking_platform.dto.request;

import lombok.Getter;
import lombok.Setter;

import java.time.OffsetDateTime;

@Getter @Setter
public class UpdateConcertRequest {
    private String title;
    private String description;
    private String venue;
    private OffsetDateTime eventDate;
    // Scope note: this endpoint updates basic info only. Status changes go
    // through the dedicated PATCH /publish endpoint, not this one — keeps
    // "publish" as an explicit, auditable action instead of a silent field
    // update. See ASSUMPTIONS.md.
}