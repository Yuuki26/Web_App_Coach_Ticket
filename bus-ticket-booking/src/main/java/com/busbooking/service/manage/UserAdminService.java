package com.busbooking.service.manage;

import com.busbooking.dto.PageParams;
import com.busbooking.dto.PageResponse;
import com.busbooking.dto.manage.UserAdminDtos;
import com.busbooking.exception.ApiException;
import com.busbooking.exception.ErrorCode;
import com.busbooking.exception.NotFoundException;
import com.busbooking.model.AuditAction;
import com.busbooking.model.Role;
import com.busbooking.model.TokenType;
import com.busbooking.model.User;
import com.busbooking.model.UserStatus;
import com.busbooking.repository.Specs;
import com.busbooking.repository.UserRepository;
import com.busbooking.repository.UserTokenRepository;
import com.busbooking.security.CurrentUser;
import com.busbooking.service.AuditService;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Locale;
import java.util.Map;

/**
 * Admin user management. Guard rails: an admin can't change their own role or deactivate
 * themselves, and the last active admin can't be demoted or deactivated.
 */
@Service
public class UserAdminService {

    static final String ENTITY = "User";
    private static final Map<String, String> SORT_FIELDS = Map.of(
            "email", "email", "fullName", "fullName", "role", "role", "status", "status",
            "createdAt", "createdAt", "lastLoginAt", "lastLoginAt", "id", "id");

    private final UserRepository users;
    private final UserTokenRepository tokens;
    private final PasswordEncoder passwordEncoder;
    private final AuditService audit;

    public UserAdminService(UserRepository users, UserTokenRepository tokens, PasswordEncoder passwordEncoder,
                            AuditService audit) {
        this.users = users;
        this.tokens = tokens;
        this.passwordEncoder = passwordEncoder;
        this.audit = audit;
    }

    @Transactional(readOnly = true)
    public PageResponse<UserAdminDtos.Response> list(UserAdminDtos.Filter f, PageParams paging) {
        Specification<User> spec = Specs.<User>containsAny(f.q(), "email", "fullName", "phone")
                .and(Specs.equalTo("role", f.role()))
                .and(Specs.equalTo("status", f.status()))
                .and(Specs.onOrAfterDay("createdAt", f.createdFrom()))
                .and(Specs.onOrBeforeDay("createdAt", f.createdTo()));
        return PageResponse.of(users.findAll(spec, paging.toPageable(SORT_FIELDS, Sort.by("email"))),
                UserAdminDtos.Response::from);
    }

    @Transactional(readOnly = true)
    public UserAdminDtos.Response get(Long id) {
        return UserAdminDtos.Response.from(find(id));
    }

    @Transactional
    public UserAdminDtos.Response create(UserAdminDtos.Create req) {
        String email = req.email().trim().toLowerCase(Locale.ROOT);
        if (users.existsByEmail(email)) {
            throw new ApiException(ErrorCode.EMAIL_TAKEN, "An account with this email already exists");
        }
        User user = new User(email, passwordEncoder.encode(req.password()), req.fullName().trim(),
                req.phone() == null || req.phone().isBlank() ? null : req.phone().trim(), req.role(), UserStatus.ACTIVE);
        user.markEmailVerified();
        user = users.save(user);
        audit.record(ENTITY, user.getId(), AuditAction.CREATE, email + " as " + req.role());
        return UserAdminDtos.Response.from(user);
    }

    @Transactional
    public UserAdminDtos.Response changeRole(Long id, Role role) {
        User user = find(id);
        notSelf(user, "change your own role");
        if (user.getRole() == role) return UserAdminDtos.Response.from(user);
        if (user.getRole() == Role.ADMIN) keepOneAdmin(user);
        String old = user.getRole().name();
        user.setRole(role);
        // Old access tokens still carry the old role until they expire (max 15 min);
        // revoking refresh tokens makes sure no new token is issued with it.
        tokens.revokeAll(user, TokenType.REFRESH, LocalDateTime.now());
        audit.record(ENTITY, id, AuditAction.ROLE_CHANGE, user.getEmail() + ": " + old + " → " + role);
        return UserAdminDtos.Response.from(user);
    }

    /** Soft delete: the account can't log in and is logged out everywhere; bookings are kept. */
    @Transactional
    public UserAdminDtos.Response deactivate(Long id) {
        User user = find(id);
        notSelf(user, "deactivate your own account");
        if (user.getStatus() == UserStatus.DEACTIVATED) return UserAdminDtos.Response.from(user);
        if (user.getRole() == Role.ADMIN) keepOneAdmin(user);
        user.setStatus(UserStatus.DEACTIVATED);
        tokens.revokeAll(user, TokenType.REFRESH, LocalDateTime.now());
        audit.record(ENTITY, id, AuditAction.DEACTIVATE, user.getEmail());
        return UserAdminDtos.Response.from(user);
    }

    @Transactional
    public UserAdminDtos.Response reactivate(Long id) {
        User user = find(id);
        if (user.getStatus() == UserStatus.DEACTIVATED) {
            user.setStatus(user.getEmailVerifiedAt() == null ? UserStatus.PENDING_VERIFICATION : UserStatus.ACTIVE);
            audit.record(ENTITY, id, AuditAction.REACTIVATE, user.getEmail());
        }
        return UserAdminDtos.Response.from(user);
    }

    private void keepOneAdmin(User user) {
        if (user.getStatus() == UserStatus.ACTIVE && users.countByRoleAndStatus(Role.ADMIN, UserStatus.ACTIVE) <= 1) {
            throw new ApiException(ErrorCode.CONFLICT, "This is the last active admin. Make someone else an admin first");
        }
    }

    private static void notSelf(User user, String what) {
        if (CurrentUser.id().filter(user.getId()::equals).isPresent()) {
            throw new ApiException(ErrorCode.CONFLICT, "You can't " + what + ". Ask another admin");
        }
    }

    private User find(Long id) {
        return users.findById(id).orElseThrow(() -> new NotFoundException("User " + id + " not found"));
    }
}
