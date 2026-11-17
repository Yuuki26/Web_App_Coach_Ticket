package com.busbooking.dto.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** Request bodies for /api/v1/auth and /api/v1/users/me. */
public final class AuthRequests {

    private AuthRequests() {}

    private static final String PHONE_REGEX = "^[+0-9 ()\\-]{6,30}$";
    private static final String PHONE_MESSAGE = "must be a phone number (digits, spaces, + - ( ) only)";

    public record Register(
            @NotBlank @Email @Size(max = 150) String email,
            @NotBlank @Pattern(regexp = PasswordRules.REGEX, message = PasswordRules.MESSAGE) String password,
            @NotBlank @Size(max = 100) String fullName,
            @Pattern(regexp = PHONE_REGEX, message = PHONE_MESSAGE) String phone
    ) {}

    public record Login(@NotBlank String email, @NotBlank String password) {}

    /** Body of verify-email: the token from the emailed link. */
    public record VerifyEmail(@NotBlank String token) {}

    public record ResendVerification(@NotBlank @Email String email) {}

    public record Refresh(@NotBlank String refreshToken) {}

    public record Logout(@NotBlank String refreshToken) {}

    /** Fields left out (null) are not changed. */
    public record UpdateProfile(
            @Size(min = 1, max = 100) String fullName,
            @Pattern(regexp = PHONE_REGEX, message = PHONE_MESSAGE) String phone
    ) {}

    public record ChangePassword(
            @NotBlank String currentPassword,
            @NotBlank @Pattern(regexp = PasswordRules.REGEX, message = PasswordRules.MESSAGE) String newPassword
    ) {}
}
