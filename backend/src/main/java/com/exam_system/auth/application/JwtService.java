package com.exam_system.auth.application;

import com.exam_system.config.JwtProperties;
import com.exam_system.user.domain.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.time.Instant;
import java.util.Date;
import java.util.Map;

@Service
public class JwtService {

    private final JwtProperties jwtProperties;
    private final SecretKey signingKey;

    public JwtService(JwtProperties jwtProperties) {
        this.jwtProperties = jwtProperties;
        this.signingKey = Keys.hmacShaKeyFor(Decoders.BASE64.decode(jwtProperties.getSecret()));
    }

    public String generateAccessToken(User user, String sessionId, String jti, Instant issuedAt, Instant expiresAt) {
        return generateToken(user, sessionId, jti, issuedAt, expiresAt, "access");
    }

    public String generateRefreshToken(User user, String sessionId, String jti, Instant issuedAt, Instant expiresAt) {
        return generateToken(user, sessionId, jti, issuedAt, expiresAt, "refresh");
    }

    private String generateToken(User user, String sessionId, String jti, Instant issuedAt, Instant expiresAt, String type) {
        Map<String, Object> claims = Map.of(
                "username", user.getUsername(),
                "role", user.getRole().getName(),
                "sid", sessionId,
                "jti", jti,
                "type", type
        );

        return Jwts.builder()
                .claims(claims)
                .subject(String.valueOf(user.getId()))
                .issuer(jwtProperties.getIssuer())
                .issuedAt(Date.from(issuedAt))
                .expiration(Date.from(expiresAt))
                .signWith(signingKey)
                .compact();
    }

    public Claims parseClaims(String token) {
        try {
            return Jwts.parser()
                    .verifyWith(signingKey)
                    .requireIssuer(jwtProperties.getIssuer())
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
        } catch (JwtException | IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid token", e);
        }
    }
}
