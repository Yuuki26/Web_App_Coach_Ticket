package com.busbooking.security;

import com.busbooking.model.User;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import java.util.List;

/**
 * Makes the two kinds of secrets a logged-in user holds:
 * <ul>
 *   <li>a short-lived signed JWT <b>access token</b> sent on every request, and</li>
 *   <li>a long random <b>opaque token</b> (refresh / email verification) that is only ever
 *       stored as its SHA-256 hash.</li>
 * </ul>
 */
@Service
public class TokenService {

    private final JwtEncoder encoder;
    private final Duration accessTokenTtl;
    private final SecureRandom random = new SecureRandom();

    public TokenService(JwtEncoder encoder,
                        @Value("${app.jwt.access-token-ttl:15m}") Duration accessTokenTtl) {
        this.encoder = encoder;
        this.accessTokenTtl = accessTokenTtl;
    }

    /** Subject is the user id (stable even if the email changes); the role goes in "roles". */
    public String createAccessToken(User user) {
        Instant now = Instant.now();
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(JwtConfig.ISSUER)
                .issuedAt(now)
                .expiresAt(now.plus(accessTokenTtl))
                .subject(String.valueOf(user.getId()))
                .claim("email", user.getEmail())
                .claim("roles", List.of(user.getRole().name()))
                .build();
        JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();
        return encoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
    }

    public Duration accessTokenTtl() {
        return accessTokenTtl;
    }

    /** 256 random bits, URL-safe so it can go in a link. */
    public String newOpaqueToken() {
        byte[] bytes = new byte[32];
        random.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    public static String hash(String token) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(token.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }
}
