package com.busbooking.dto.auth;

import com.busbooking.model.Role;
import com.busbooking.model.User;
import com.busbooking.model.UserStatus;

import java.time.LocalDateTime;

/** A user as the API shows it. Never includes the password hash. */
public record UserResponse(
        Long id,
        String email,
        String fullName,
        String phone,
        Role role,
        UserStatus status,
        boolean emailVerified,
        LocalDateTime lastLoginAt,
        LocalDateTime createdAt
) {
    public static UserResponse from(User u) {
        return new UserResponse(u.getId(), u.getEmail(), u.getFullName(), u.getPhone(), u.getRole(),
                u.getStatus(), u.getEmailVerifiedAt() != null, u.getLastLoginAt(), u.getCreatedAt());
    }
}
