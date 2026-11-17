package com.busbooking.model;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * A one-off secret handed to a user: a refresh token or an email verification link.
 * Only a SHA-256 hash of the secret is stored, so a leaked database cannot be used to log in.
 */
@Entity
@Table(name = "user_tokens", indexes = @Index(columnList = "user_id, type"))
public class UserToken {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private TokenType type;

    @Column(nullable = false, unique = true, length = 64)
    private String tokenHash;

    @Column(nullable = false)
    private LocalDateTime expiresAt;

    /** Set when the token is used up or revoked (logout, password change, refresh rotation). */
    private LocalDateTime usedAt;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    protected UserToken() {}

    public UserToken(User user, TokenType type, String tokenHash, LocalDateTime expiresAt) {
        this.user = user;
        this.type = type;
        this.tokenHash = tokenHash;
        this.expiresAt = expiresAt;
    }

    public boolean isUsable() {
        return usedAt == null && expiresAt.isAfter(LocalDateTime.now());
    }

    public void markUsed() {
        if (usedAt == null) usedAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public User getUser() { return user; }
    public TokenType getType() { return type; }
    public LocalDateTime getExpiresAt() { return expiresAt; }
    public LocalDateTime getUsedAt() { return usedAt; }
}
