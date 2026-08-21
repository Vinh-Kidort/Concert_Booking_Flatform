package com.ticketbooking.concert_booking_platform.controller;

import com.ticketbooking.concert_booking_platform.common.ApiResponse;
import com.ticketbooking.concert_booking_platform.dto.response.ConcertResponse;
import com.ticketbooking.concert_booking_platform.dto.response.TicketCategoryResponse;
import com.ticketbooking.concert_booking_platform.entity.Concert;
import com.ticketbooking.concert_booking_platform.service.ConcertService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/concerts")
@RequiredArgsConstructor
@Tag(name = "2. Customer Concerts", description = "Browse on-sale concerts and ticket categories")
public class ConcertController {

    private final ConcertService concertService;

    @GetMapping
    @Operation(summary = "Browse concerts on sale", description = "Public endpoint to list all concerts currently ON_SALE with pagination.")
    public ApiResponse<Page<ConcertResponse>> browseConcerts(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<ConcertResponse> result = concertService.browseOnSaleConcerts(pageable)
                .map(ConcertResponse::summary);
        return ApiResponse.success(result);
    }

    @GetMapping("/{concertId}")
    @Operation(summary = "Get concert details", description = "View detailed information of a concert along with live ticket category availability.")
    public ApiResponse<ConcertResponse> getConcertDetail(@PathVariable Long concertId) {
        Concert concert = concertService.getConcertDetail(concertId);
        ConcertResponse response = ConcertResponse.summary(concert).toBuilder()
                .ticketCategories(concertService.getTicketCategories(concertId).stream()
                        .map(TicketCategoryResponse::from)
                        .toList())
                .build();
        return ApiResponse.success(response);
    }
}