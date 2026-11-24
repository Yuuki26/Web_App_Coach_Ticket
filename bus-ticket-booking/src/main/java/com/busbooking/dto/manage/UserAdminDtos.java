package com.busbooking.dto.manage;

import com.busbooking.dto.AuditInfo;
import com.busbooking.dto.auth.PasswordRules;
import com.busbooking.dto.auth.UserResponse;
import com.busbooking.model.Role;
import com.busbooking.model.User;
import com.busbooking.model.UserStatus;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public final class UserAdminDtos {

    private UserAdminDtos() {}

    /** An admin creates an account directly; it is active and verified straight away. */
    public record Create(
            @NotBlank @Email @Size(max = 150) String email,
            @NotBlank @Pattern(regexp = PasswordRules.REGEX, message = PasswordRules.MESSAGE) String password,
            @NotBlank @Size(max = 100) String fullName,
            @Pattern(regexp = "^[+0-9 ()\\-]{6,30}$", message = "must be a phone number") String phone,
            @NotNull Role role
    ) {}

    public record ChangeRole(@NotNull Role role) {}

    /** {@code q} matches email, name or phone. Dates are whole days. */
    public record Filter(String q, Role role, UserStatus status, LocalDate createdFrom, LocalDate createdTo) {}

    public record Response(UserResponse user, AuditInfo audit) {
        public static Response from(User u) {
            return new Response(UserResponse.from(u), AuditInfo.of(u));
        }
    }
}
