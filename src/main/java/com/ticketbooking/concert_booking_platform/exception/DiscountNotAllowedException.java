package com.ticketbooking.concert_booking_platform.exception;

public class DiscountNotAllowedException extends RuntimeException {
    public DiscountNotAllowedException(String message) {
        super(message);
    }
}