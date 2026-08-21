package com.ticketbooking.concert_booking_platform.exception;

public class InsufficientTicketException extends RuntimeException {
    public InsufficientTicketException(String message) { super(message); }
}