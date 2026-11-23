package com.busbooking.model;

public enum CoachStatus {
    /** In service: can be scheduled on trips. */
    ACTIVE,
    /** Temporarily out of service: cannot be put on new trips. */
    MAINTENANCE,
    /** Permanently out of service (soft delete). Kept for the history of past trips. */
    RETIRED
}
