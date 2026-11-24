package com.busbooking.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import java.util.Optional;

/** Who is calling, read from the access token of the current request. */
public final class CurrentUser {

    /** Recorded as the actor when there is no logged-in user (startup data, guests, sign-up). */
    public static final String SYSTEM = "system";

    private CurrentUser() {}

    /** The logged-in user's id, or empty for a guest. */
    public static Optional<Long> id() {
        return token().map(jwt -> Long.valueOf(jwt.getToken().getSubject()));
    }

    /** The logged-in user's email (from the token), or empty for a guest. */
    public static Optional<String> email() {
        return token().map(jwt -> jwt.getToken().getClaimAsString("email"));
    }

    private static Optional<JwtAuthenticationToken> token() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return auth instanceof JwtAuthenticationToken jwt ? Optional.of(jwt) : Optional.empty();
    }
}
