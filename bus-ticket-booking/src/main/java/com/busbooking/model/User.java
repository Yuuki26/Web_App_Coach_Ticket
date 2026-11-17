package com.busbooking.model;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "users", indexes = @Index(columnList = "role, status"))
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Login name. Always stored lower-case so lookups are case-insensitive. */
    @Column(nullable = false, unique = true, length = 150)
    private String email;

    /** BCrypt hash. The plain password is never stored. */
    @Column(nullable = false, length = 100)
    private String passwordHash;

    @Column(nullable = false, length = 100)
    private String fullName;

    @Column(length = 30)
    private String phone;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Role role;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private UserStatus status;

    private LocalDateTime emailVerifiedAt;

    private LocalDateTime lastLoginAt;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(nullable = false)
    private LocalDateTime updatedAt;

    protected User() {}

    public User(String email, String passwordHash, String fullName, String phone, Role role, UserStatus status) {
        this.email = email;
        this.passwordHash = passwordHash;
        this.fullName = fullName;
        this.phone = phone;
        this.role = role;
        this.status = status;
    }

    public boolean isActive() { return status == UserStatus.ACTIVE; }

    public void markEmailVerified() {
        this.emailVerifiedAt = LocalDateTime.now();
        if (status == UserStatus.PENDING_VERIFICATION) this.status = UserStatus.ACTIVE;
    }

    public Long getId() { return id; }
    public String getEmail() { return email; }
    public String getPasswordHash() { return passwordHash; }
    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }
    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    public Role getRole() { return role; }
    public void setRole(Role role) { this.role = role; }
    public UserStatus getStatus() { return status; }
    public void setStatus(UserStatus status) { this.status = status; }
    public LocalDateTime getEmailVerifiedAt() { return emailVerifiedAt; }
    public LocalDateTime getLastLoginAt() { return lastLoginAt; }
    public void setLastLoginAt(LocalDateTime lastLoginAt) { this.lastLoginAt = lastLoginAt; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}
