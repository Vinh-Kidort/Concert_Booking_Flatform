package com.ticketbooking.concert_booking_platform.dto.response;

import com.ticketbooking.concert_booking_platform.entity.Concert;
import com.ticketbooking.concert_booking_platform.enums.ConcertStatus;
import lombok.Builder;
import lombok.Getter;

import java.time.OffsetDateTime;
import java.util.List;

@Getter
@Builder(toBuilder = true)
public class ConcertResponse {
    private Long id;
    private String title;
    private String description;
    private String venue;
    private OffsetDateTime eventDate;
    private ConcertStatus status;
    private List<TicketCategoryResponse> ticketCategories; // null when listing, populated on detail view

    public static ConcertResponse summary(Concert c) {
        return ConcertResponse.builder()
                .id(c.getId())
                .title(c.getTitle())
                .description(c.getDescription())
                .venue(c.getVenue())
                .eventDate(c.getEventDate())
                .status(c.getStatus())
                .build();
    }
}