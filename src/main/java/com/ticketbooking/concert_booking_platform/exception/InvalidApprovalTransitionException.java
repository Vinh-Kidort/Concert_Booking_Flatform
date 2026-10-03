package com.ticketbooking.concert_booking_platform.exception;

public class InvalidApprovalTransitionException extends RuntimeException {
    public InvalidApprovalTransitionException(String message) {
        super(message);
    }
}