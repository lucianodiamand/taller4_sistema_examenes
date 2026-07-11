package com.exam_system.auth.application;

import com.exam_system.config.JwtProperties;
import com.exam_system.user.domain.Role;
import com.exam_system.user.domain.User;
import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;

class JwtServiceTest {

    @Test
    void generateAndParseAccessToken() {
        JwtProperties properties = new JwtProperties();
        properties.setIssuer("exam-system-test");
        properties.setSecret("VGhpc0lzQVRlc3RTZWNyZXRLZXlGb3JKV1RfMzJfQnl0ZXNfTWluaW11bQ==");

        JwtService jwtService = new JwtService(properties);

        Role role = new Role();
        role.setName("STUDENT");
        User user = new User();
        user.setId(7L);
        user.setUsername("alice");
        user.setRole(role);

        Instant issuedAt = Instant.now();
        Instant expiresAt = issuedAt.plusSeconds(900);
        String token = jwtService.generateAccessToken(user, "sid-1", "jti-1", issuedAt, expiresAt);

        Claims claims = jwtService.parseClaims(token);

        assertEquals("7", claims.getSubject());
        assertEquals("alice", claims.get("username", String.class));
        assertEquals("STUDENT", claims.get("role", String.class));
        assertEquals("access", claims.get("type", String.class));
        assertEquals("jti-1", claims.get("jti", String.class));
        assertEquals("sid-1", claims.get("sid", String.class));
    }
}
