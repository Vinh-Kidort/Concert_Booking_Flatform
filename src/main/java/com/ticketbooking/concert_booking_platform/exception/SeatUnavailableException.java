package com.ticketbooking.concert_booking_platform.exception;

public class SeatUnavailableException extends RuntimeException {
    public SeatUnavailableException(String message) { super(message); }
}