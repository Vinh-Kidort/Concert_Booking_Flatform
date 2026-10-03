package com.ticketbooking.concert_booking_platform.controller.organizer;

import com.ticketbooking.concert_booking_platform.common.ApiResponse;
import com.ticketbooking.concert_booking_platform.dto.request.IssueCompTicketRequest;
import com.ticketbooking.concert_booking_platform.dto.response.CompTicketResponse;
import com.ticketbooking.concert_booking_platform.dto.response.SeatAllocationReportResponse;
import com.ticketbooking.concert_booking_platform.entity.CompTicket;
import com.ticketbooking.concert_booking_platform.security.CurrentUserProvider;
import com.ticketbooking.concert_booking_platform.service.CompTicketService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/organizer")
@RequiredArgsConstructor
public class OrganizerCompTicketController {

    private final CompTicketService compTicketService;
    private final CurrentUserProvider currentUserProvider;

    @PostMapping("/ticket-categories/{ticketCategoryId}/comp-tickets")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<CompTicketResponse> issueCompTicket(
            @PathVariable Long ticketCategoryId,
            @Valid @RequestBody IssueCompTicketRequest request) {
        Long organizerId = currentUserProvider.getCurrentUserId();
        CompTicket ticket = compTicketService.issueCompTicket(organizerId, ticketCategoryId, request);
        return ApiResponse.success(CompTicketResponse.from(ticket));
    }

    @DeleteMapping("/comp-tickets/{compTicketId}")
    public ApiResponse<Void> revokeCompTicket(@PathVariable Long compTicketId) {
        Long organizerId = currentUserProvider.getCurrentUserId();
        compTicketService.revokeCompTicket(organizerId, compTicketId);
        return ApiResponse.success(null);
    }

    @GetMapping("/ticket-categories/{ticketCategoryId}/comp-tickets")
    public ApiResponse<Page<CompTicketResponse>> listCompTickets(
            @PathVariable Long ticketCategoryId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Long organizerId = currentUserProvider.getCurrentUserId();
        Page<CompTicketResponse> result = compTicketService
                .listByCategory(organizerId, ticketCategoryId, PageRequest.of(page, size))
                .map(CompTicketResponse::from);
        return ApiResponse.success(result);
    }

    @GetMapping("/ticket-categories/{ticketCategoryId}/seat-allocation")
    public ApiResponse<SeatAllocationReportResponse> seatAllocationReport(@PathVariable Long ticketCategoryId) {
        Long organizerId = currentUserProvider.getCurrentUserId();
        return ApiResponse.success(compTicketService.getSeatAllocationReport(organizerId, ticketCategoryId));
    }
}