package com.ticketbooking.concert_booking_platform.exception;

public class InvalidBookingStatusTransitionException extends RuntimeException {
    public InvalidBookingStatusTransitionException(String message) { super(message); }
}