package com.ticketbooking.concert_booking_platform.controller;

import com.ticketbooking.concert_booking_platform.common.ApiResponse;
import com.ticketbooking.concert_booking_platform.dto.request.CreateBookingRequest;
import com.ticketbooking.concert_booking_platform.dto.response.BookingResponse;
import com.ticketbooking.concert_booking_platform.entity.Booking;
import com.ticketbooking.concert_booking_platform.exception.ResourceNotFoundException;
import com.ticketbooking.concert_booking_platform.repository.BookingRepository;
import com.ticketbooking.concert_booking_platform.security.CurrentUserProvider;
import com.ticketbooking.concert_booking_platform.service.BookingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/bookings")
@RequiredArgsConstructor
@Tag(name = "3. Customer Bookings", description = "Reserve tickets and view customer booking history")
@SecurityRequirement(name = "bearerAuth")
public class BookingController {

    private final BookingService bookingService;
    private final BookingRepository bookingRepository;
    private final CurrentUserProvider currentUserProvider;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create a booking (Reserve Tickets)", description = "Temporarily reserve tickets under flash sale concurrency controls. Includes Idempotency Key protection.")
    public ApiResponse<BookingResponse> createBooking(@Valid @RequestBody CreateBookingRequest request) {
        Long userId = currentUserProvider.getCurrentUserId();
        Booking booking = bookingService.createBooking(userId, request);
        return ApiResponse.success(BookingResponse.from(booking));
    }

    @GetMapping("/{bookingId}")
    @Operation(summary = "Get booking detail", description = "Retrieve a specific booking's detail. Restricted to the booking owner.")
    public ApiResponse<BookingResponse> getBooking(@PathVariable Long bookingId) {
        Long userId = currentUserProvider.getCurrentUserId();
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found: " + bookingId));
        if (!booking.getUser().getId().equals(userId)) {
            throw new ResourceNotFoundException("Booking not found: " + bookingId);
        }
        return ApiResponse.success(BookingResponse.from(booking));
    }

    @GetMapping
    @Operation(summary = "List my bookings", description = "Get a paginated list of bookings owned by the authenticated customer.")
    public ApiResponse<Page<BookingResponse>> myBookings(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Long userId = currentUserProvider.getCurrentUserId();
        var result = bookingRepository.findByUserId(userId, PageRequest.of(page, size)).map(BookingResponse::from);
        return ApiResponse.success(result);
    }
}