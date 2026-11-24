package com.busbooking.model;

public enum UserStatus {
    /** Registered but the email link has not been clicked yet. Cannot log in. */
    PENDING_VERIFICATION,
    ACTIVE,
    /** Switched off by an admin (soft delete). Cannot log in; bookings are kept. */
    DEACTIVATED
}
