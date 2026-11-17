package com.busbooking.dto.auth;

/** Shared password policy, used in @Pattern on every request that sets a password. */
public final class PasswordRules {

    /** 8–72 characters (BCrypt ignores anything past 72 bytes) with lower, upper, digit and symbol. */
    public static final String REGEX = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[^A-Za-z0-9]).{8,72}$";

    public static final String MESSAGE =
            "must be 8-72 characters and include an uppercase letter, a lowercase letter, a number and a symbol";

    private PasswordRules() {}
}
