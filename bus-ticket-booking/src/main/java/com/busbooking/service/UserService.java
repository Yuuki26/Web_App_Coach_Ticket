package com.busbooking.service;

import com.busbooking.dto.auth.AuthRequests;
import com.busbooking.dto.auth.UserResponse;
import com.busbooking.exception.ApiException;
import com.busbooking.exception.ErrorCode;
import com.busbooking.exception.NotFoundException;
import com.busbooking.model.TokenType;
import com.busbooking.model.User;
import com.busbooking.repository.UserRepository;
import com.busbooking.repository.UserTokenRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Sort;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/** The logged-in user's own profile, plus the admin's user list. */
@Service
public class UserService {

    private static final Logger log = LoggerFactory.getLogger(UserService.class);

    private final UserRepository users;
    private final UserTokenRepository tokens;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository users, UserTokenRepository tokens, PasswordEncoder passwordEncoder) {
        this.users = users;
        this.tokens = tokens;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional(readOnly = true)
    public UserResponse profile(Long userId) {
        return UserResponse.from(get(userId));
    }

    @Transactional
    public UserResponse updateProfile(Long userId, AuthRequests.UpdateProfile req) {
        User user = get(userId);
        if (req.fullName() != null) user.setFullName(req.fullName().trim());
        if (req.phone() != null) user.setPhone(AuthService.blankToNull(req.phone()));
        return UserResponse.from(user);
    }

    /** Needs the current password. Logs the user out on every device afterwards. */
    @Transactional
    public void changePassword(Long userId, AuthRequests.ChangePassword req) {
        User user = get(userId);
        if (!passwordEncoder.matches(req.currentPassword(), user.getPasswordHash())) {
            throw new ApiException(ErrorCode.WRONG_PASSWORD, "Your current password is not correct");
        }
        if (passwordEncoder.matches(req.newPassword(), user.getPasswordHash())) {
            throw new ApiException(ErrorCode.VALIDATION_FAILED, "The new password must be different from the current one");
        }
        user.setPasswordHash(passwordEncoder.encode(req.newPassword()));
        tokens.revokeAll(user, TokenType.REFRESH, LocalDateTime.now());
        log.info("Password changed for user {}", userId);
    }

    /** Admin view. Full user management (create, deactivate, change role, paging) comes in phase 2. */
    @Transactional(readOnly = true)
    public List<UserResponse> listAll() {
        return users.findAll(Sort.by("id")).stream().map(UserResponse::from).toList();
    }

    private User get(Long id) {
        return users.findById(id).orElseThrow(() -> new NotFoundException("User not found"));
    }
}
