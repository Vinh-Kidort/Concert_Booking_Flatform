package com.ticketbooking.concert_booking_platform.exception;

public class CompTicketQuotaExhaustedException extends RuntimeException {
    public CompTicketQuotaExhaustedException(String message) {
        super(message);
    }
}