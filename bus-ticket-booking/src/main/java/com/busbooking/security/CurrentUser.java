package com.busbooking.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import java.util.Optional;

/** Who is calling, read from the access token of the current request. */
public final class CurrentUser {

    private CurrentUser() {}

    /** The logged-in user's id, or empty for a guest. */
    public static Optional<Long> id() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth instanceof JwtAuthenticationToken jwt) {
            return Optional.of(Long.valueOf(jwt.getToken().getSubject()));
        }
        return Optional.empty();
    }
}
