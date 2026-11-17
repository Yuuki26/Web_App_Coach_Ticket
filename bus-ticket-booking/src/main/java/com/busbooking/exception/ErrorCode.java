package com.busbooking.exception;

import org.springframework.http.HttpStatus;

/**
 * Machine-readable error codes sent in every error response as {@code "code"}, so the
 * frontend can react to a specific problem without parsing the human-readable message.
 */
public enum ErrorCode {
    // 400
    VALIDATION_FAILED(HttpStatus.BAD_REQUEST),
    BAD_REQUEST(HttpStatus.BAD_REQUEST),
    INVALID_TOKEN(HttpStatus.BAD_REQUEST),
    WRONG_PASSWORD(HttpStatus.BAD_REQUEST),
    // 401
    UNAUTHORIZED(HttpStatus.UNAUTHORIZED),
    INVALID_CREDENTIALS(HttpStatus.UNAUTHORIZED),
    REFRESH_TOKEN_INVALID(HttpStatus.UNAUTHORIZED),
    // 403
    FORBIDDEN(HttpStatus.FORBIDDEN),
    EMAIL_NOT_VERIFIED(HttpStatus.FORBIDDEN),
    ACCOUNT_DISABLED(HttpStatus.FORBIDDEN),
    // 404
    NOT_FOUND(HttpStatus.NOT_FOUND),
    // 409
    CONFLICT(HttpStatus.CONFLICT),
    EMAIL_TAKEN(HttpStatus.CONFLICT),
    SEAT_TAKEN(HttpStatus.CONFLICT),
    TRIP_DEPARTED(HttpStatus.CONFLICT),
    BOOKING_RULE_VIOLATED(HttpStatus.CONFLICT),
    // 500
    INTERNAL_ERROR(HttpStatus.INTERNAL_SERVER_ERROR);

    private final HttpStatus status;

    ErrorCode(HttpStatus status) {
        this.status = status;
    }

    public HttpStatus status() {
        return status;
    }
}
