package com.busbooking.dto.auth;

/**
 * Returned by login and refresh. Send {@code accessToken} as {@code Authorization: Bearer ...};
 * when it expires ({@code expiresIn} seconds), call /auth/refresh with {@code refreshToken}.
 */
public record AuthResponse(
        String accessToken,
        String tokenType,
        long expiresIn,
        String refreshToken,
        UserResponse user
) {
}
