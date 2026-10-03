package com.ticketbooking.concert_booking_platform.exception;

public class RefundNotAllowedException extends RuntimeException {
    public RefundNotAllowedException(String message) { super(message); }
}