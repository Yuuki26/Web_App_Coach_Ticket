package com.busbooking.service;

import com.busbooking.dto.auth.AuthRequests;
import com.busbooking.dto.auth.AuthResponse;
import com.busbooking.dto.auth.UserResponse;
import com.busbooking.exception.ApiException;
import com.busbooking.exception.ErrorCode;
import com.busbooking.model.Role;
import com.busbooking.model.TokenType;
import com.busbooking.model.User;
import com.busbooking.model.UserStatus;
import com.busbooking.model.UserToken;
import com.busbooking.repository.UserRepository;
import com.busbooking.repository.UserTokenRepository;
import com.busbooking.security.TokenService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Locale;

/**
 * Registration, email verification, login, token refresh and logout.
 *
 * <p>Login hands out a short-lived JWT access token plus a refresh token. Refresh tokens are
 * single-use: each refresh revokes the old one and issues a new pair ("rotation"). If a refresh
 * token that was already used shows up again, it may have been stolen, so every refresh token of
 * that user is revoked and they must log in again.
 */
@Service
public class AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);

    private final UserRepository users;
    private final UserTokenRepository tokens;
    private final PasswordEncoder passwordEncoder;
    private final TokenService tokenService;
    private final AccountMailer mailer;
    private final Duration refreshTokenTtl;
    private final Duration verificationTtl;
    private final boolean requireEmailVerification;
    private final String verifyEmailUrl;

    /** Compared against when the email is unknown, so a wrong email takes as long as a wrong password. */
    private final String dummyHash;

    public AuthService(UserRepository users, UserTokenRepository tokens, PasswordEncoder passwordEncoder,
                       TokenService tokenService, AccountMailer mailer,
                       @Value("${app.jwt.refresh-token-ttl:7d}") Duration refreshTokenTtl,
                       @Value("${app.auth.verification-ttl:24h}") Duration verificationTtl,
                       @Value("${app.auth.require-email-verification:true}") boolean requireEmailVerification,
                       @Value("${app.auth.verify-email-url}") String verifyEmailUrl) {
        this.users = users;
        this.tokens = tokens;
        this.passwordEncoder = passwordEncoder;
        this.tokenService = tokenService;
        this.mailer = mailer;
        this.refreshTokenTtl = refreshTokenTtl;
        this.verificationTtl = verificationTtl;
        this.requireEmailVerification = requireEmailVerification;
        this.verifyEmailUrl = verifyEmailUrl;
        this.dummyHash = passwordEncoder.encode("not-a-real-password");
    }

    /** New accounts are always customers. Staff accounts are created by an admin. */
    @Transactional
    public UserResponse register(AuthRequests.Register req) {
        String email = normalizeEmail(req.email());
        if (users.existsByEmail(email)) {
            throw new ApiException(ErrorCode.EMAIL_TAKEN, "An account with this email already exists");
        }
        UserStatus status = requireEmailVerification ? UserStatus.PENDING_VERIFICATION : UserStatus.ACTIVE;
        User user = users.save(new User(email, passwordEncoder.encode(req.password()), req.fullName().trim(),
                blankToNull(req.phone()), Role.CUSTOMER, status));
        if (requireEmailVerification) {
            sendVerification(user);
        } else {
            user.markEmailVerified();
        }
        log.info("Registered user {}", user.getId());
        return UserResponse.from(user);
    }

    @Transactional
    public void verifyEmail(String token) {
        UserToken t = tokens.findByTokenHashAndType(TokenService.hash(token), TokenType.EMAIL_VERIFICATION)
                .filter(UserToken::isUsable)
                .orElseThrow(() -> new ApiException(ErrorCode.INVALID_TOKEN,
                        "This verification link is invalid or has expired. Request a new one"));
        t.markUsed();
        t.getUser().markEmailVerified();
        log.info("Email verified for user {}", t.getUser().getId());
    }

    /** Always succeeds from the caller's view, so it can't be used to find out which emails exist. */
    @Transactional
    public void resendVerification(String email) {
        users.findByEmail(normalizeEmail(email))
                .filter(u -> u.getStatus() == UserStatus.PENDING_VERIFICATION)
                .ifPresent(this::sendVerification);
    }

    @Transactional
    public AuthResponse login(AuthRequests.Login req) {
        User user = users.findByEmail(normalizeEmail(req.email())).orElse(null);
        String hash = user != null ? user.getPasswordHash() : dummyHash;
        boolean passwordOk = passwordEncoder.matches(req.password(), hash);
        if (user == null || !passwordOk) {
            log.warn("Login failed: wrong email or password");
            throw new ApiException(ErrorCode.INVALID_CREDENTIALS, "Wrong email or password");
        }
        // Status is only revealed to someone who knows the password.
        checkCanLogIn(user);
        user.setLastLoginAt(LocalDateTime.now());
        log.info("Login succeeded for user {}", user.getId());
        return issueTokens(user);
    }

    @Transactional(noRollbackFor = ApiException.class)
    public AuthResponse refresh(String refreshToken) {
        UserToken t = tokens.findByTokenHashAndType(TokenService.hash(refreshToken), TokenType.REFRESH)
                .orElseThrow(AuthService::invalidRefresh);
        User user = t.getUser();
        if (t.getUsedAt() != null) {
            int revoked = tokens.revokeAll(user, TokenType.REFRESH, LocalDateTime.now());
            log.warn("Reused refresh token for user {}: revoked {} session(s)", user.getId(), revoked);
            throw invalidRefresh();
        }
        if (!t.isUsable()) throw invalidRefresh();
        checkCanLogIn(user);
        t.markUsed();
        return issueTokens(user);
    }

    /** Ends this device's session. Unknown or already-used tokens are ignored. */
    @Transactional
    public void logout(String refreshToken) {
        tokens.findByTokenHashAndType(TokenService.hash(refreshToken), TokenType.REFRESH)
                .ifPresent(UserToken::markUsed);
    }

    private AuthResponse issueTokens(User user) {
        String refresh = tokenService.newOpaqueToken();
        tokens.save(new UserToken(user, TokenType.REFRESH, TokenService.hash(refresh),
                LocalDateTime.now().plus(refreshTokenTtl)));
        return new AuthResponse(tokenService.createAccessToken(user), "Bearer",
                tokenService.accessTokenTtl().toSeconds(), refresh, UserResponse.from(user));
    }

    private void sendVerification(User user) {
        tokens.revokeAll(user, TokenType.EMAIL_VERIFICATION, LocalDateTime.now());
        String token = tokenService.newOpaqueToken();
        tokens.save(new UserToken(user, TokenType.EMAIL_VERIFICATION, TokenService.hash(token),
                LocalDateTime.now().plus(verificationTtl)));
        mailer.sendVerificationLink(user, verifyEmailUrl + token);
    }

    private static void checkCanLogIn(User user) {
        switch (user.getStatus()) {
            case PENDING_VERIFICATION -> throw new ApiException(ErrorCode.EMAIL_NOT_VERIFIED,
                    "Please verify your email first. Check your inbox for the link");
            case DEACTIVATED -> throw new ApiException(ErrorCode.ACCOUNT_DISABLED,
                    "This account has been deactivated. Contact support");
            case ACTIVE -> { }
        }
    }

    private static ApiException invalidRefresh() {
        return new ApiException(ErrorCode.REFRESH_TOKEN_INVALID, "Your session has expired. Please log in again");
    }

    static String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }

    static String blankToNull(String s) {
        return s == null || s.isBlank() ? null : s.trim();
    }
}
