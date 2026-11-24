package com.busbooking.exception;

/** Business-rule violation while booking, e.g. seat already taken or invalid seat. */
public class BookingException extends ApiException {

    public BookingException(String message) {
        super(ErrorCode.BOOKING_RULE_VIOLATED, message);
    }

    public BookingException(ErrorCode code, String message) {
        super(code, message);
    }
}
