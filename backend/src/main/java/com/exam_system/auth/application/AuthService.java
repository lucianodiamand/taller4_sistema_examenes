package com.exam_system.auth.application;

import com.exam_system.auth.domain.AuthToken;
import com.exam_system.auth.repository.AuthTokenRepository;
import com.exam_system.auth.security.CurrentUser;
import com.exam_system.config.JwtProperties;
import com.exam_system.user.domain.Role;
import com.exam_system.user.domain.User;
import com.exam_system.user.repository.RoleRepository;
import com.exam_system.user.repository.UserRepository;
import io.jsonwebtoken.Claims;
import jakarta.persistence.EntityNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
public class AuthService {

    private static final Logger logger = LoggerFactory.getLogger(AuthService.class);

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final AuthTokenRepository authTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final JwtProperties jwtProperties;
    private final CurrentUser currentUser;

    public AuthService(UserRepository userRepository,
                       RoleRepository roleRepository,
                       AuthTokenRepository authTokenRepository,
                       PasswordEncoder passwordEncoder,
                       JwtService jwtService,
                       JwtProperties jwtProperties,
                       CurrentUser currentUser) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.authTokenRepository = authTokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.jwtProperties = jwtProperties;
        this.currentUser = currentUser;
    }

    @Transactional
    public void registerStudent(String name, String username, String rawPassword) {
        if (userRepository.existsByUsername(username)) {
            throw new IllegalArgumentException("Username already exists");
        }

        Role studentRole = roleRepository.findByName("STUDENT")
                .orElseThrow(() -> new EntityNotFoundException("Role STUDENT not found"));

        User user = new User();
        user.setName(name);
        user.setUsername(username);
        user.setPassword(passwordEncoder.encode(rawPassword));
        user.setRole(studentRole);
        userRepository.save(user);
        logger.info("Registered student user={}", username);
    }

    @Transactional
    public TokenPair login(String username, String rawPassword, String ip, String userAgent) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new EntityNotFoundException("User not found"));

        if (!passwordEncoder.matches(rawPassword, user.getPassword())) {
            throw new BadCredentialsException("Invalid credentials");
        }

        logger.info("Login success userId={} role={}", user.getId(), user.getRole().getName());
        return issueTokenPair(user, ip, userAgent);
    }

    @Transactional
    public TokenPair refresh(String refreshToken, String ip, String userAgent) {
        Claims claims = jwtService.parseClaims(refreshToken);
        if (!"refresh".equals(claims.get("type", String.class))) {
            throw new IllegalArgumentException("Invalid refresh token");
        }

        String refreshJti = claims.get("jti", String.class);
        AuthToken stored = authTokenRepository.findByRefreshJti(refreshJti)
                .orElseThrow(() -> new IllegalArgumentException("Refresh token not found"));

        if (stored.getRevokedAt() != null || stored.getRefreshExpiresAt().isBefore(Instant.now())) {
            throw new IllegalArgumentException("Refresh token is revoked or expired");
        }

        User user = stored.getUser();
        TokenPair newPair = issueTokenPair(user, ip, userAgent);
        AuthToken replacement = authTokenRepository.findByRefreshJti(newPair.refreshJti())
                .orElseThrow(() -> new IllegalStateException("Replacement token not found"));

        stored.setRevokedAt(Instant.now());
        stored.setReplacedByToken(replacement);
        authTokenRepository.save(stored);

        logger.info("Refresh success userId={}", user.getId());
        return newPair;
    }

    @Transactional
    public void logout(String accessToken) {
        Claims claims = jwtService.parseClaims(accessToken);
        String jti = claims.get("jti", String.class);
        authTokenRepository.findByAccessJti(jti).ifPresent(token -> {
            token.setRevokedAt(Instant.now());
            authTokenRepository.save(token);
            logger.info("Logout success userId={} sessionId={}", token.getUser().getId(), token.getSessionId());
        });
    }

    @Transactional
    public void logoutAllCurrentUserSessions() {
        Long userId = currentUser.id();
        List<AuthToken> activeTokens = authTokenRepository.findByUserIdAndRevokedAtIsNull(userId);
        Instant now = Instant.now();
        for (AuthToken token : activeTokens) {
            token.setRevokedAt(now);
        }
        authTokenRepository.saveAll(activeTokens);
        logger.info("Logout-all success userId={} sessionsRevoked={}", userId, activeTokens.size());
    }

    private TokenPair issueTokenPair(User user, String ip, String userAgent) {
        Instant now = Instant.now();
        String sessionId = UUID.randomUUID().toString();
        String accessJti = UUID.randomUUID().toString();
        String refreshJti = UUID.randomUUID().toString();
        Instant accessExpiresAt = now.plus(jwtProperties.getAccessTtl());
        Instant refreshExpiresAt = now.plus(jwtProperties.getRefreshTtl());

        String accessToken = jwtService.generateAccessToken(user, sessionId, accessJti, now, accessExpiresAt);
        String refreshToken = jwtService.generateRefreshToken(user, sessionId, refreshJti, now, refreshExpiresAt);

        AuthToken authToken = new AuthToken();
        authToken.setUser(user);
        authToken.setAccessToken(accessToken);
        authToken.setRefreshToken(refreshToken);
        authToken.setAccessJti(accessJti);
        authToken.setRefreshJti(refreshJti);
        authToken.setSessionId(sessionId);
        authToken.setIssuedAt(now);
        authToken.setAccessExpiresAt(accessExpiresAt);
        authToken.setRefreshExpiresAt(refreshExpiresAt);
        authToken.setCreatedIp(ip);
        authToken.setCreatedUserAgent(userAgent);
        authTokenRepository.save(authToken);

        return new TokenPair(accessToken, refreshToken, accessExpiresAt, refreshExpiresAt, refreshJti);
    }

    public record TokenPair(
            String accessToken,
            String refreshToken,
            Instant accessExpiresAt,
            Instant refreshExpiresAt,
            String refreshJti
    ) {
    }
}
