package com.busbooking.exception;

/** Business-rule violation, e.g. seat already taken or invalid seat number. */
public class BookingException extends RuntimeException {
    public BookingException(String message) {
        super(message);
    }
}
