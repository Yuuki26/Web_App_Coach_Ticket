package com.busbooking.repository;

import com.busbooking.model.TokenType;
import com.busbooking.model.User;
import com.busbooking.model.UserToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Optional;

public interface UserTokenRepository extends JpaRepository<UserToken, Long> {

    Optional<UserToken> findByTokenHashAndType(String tokenHash, TokenType type);

    /** Revokes every still-open token of one type for a user, e.g. all refresh tokens on password change. */
    @Modifying
    @Query("update UserToken t set t.usedAt = :now where t.user = :user and t.type = :type and t.usedAt is null")
    int revokeAll(@Param("user") User user, @Param("type") TokenType type, @Param("now") LocalDateTime now);
}
