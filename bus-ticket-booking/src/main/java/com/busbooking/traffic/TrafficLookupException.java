package com.busbooking.traffic;

/** A traffic provider couldn't answer. The message is safe to show to users. */
public class TrafficLookupException extends RuntimeException {
    public TrafficLookupException(String message) {
        super(message);
    }

    public TrafficLookupException(String message, Throwable cause) {
        super(message, cause);
    }
}
