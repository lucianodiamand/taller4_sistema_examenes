package com.exam_system.auth.api;

import com.exam_system.auth.application.AuthService;
import com.exam_system.auth.application.AuthService.TokenPair;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public void register(@Valid @RequestBody RegisterRequest request) {
        authService.registerStudent(request.name(), request.username(), request.password());
    }

    @PostMapping("/login")
    public TokenResponse login(@Valid @RequestBody LoginRequest request,
                               @RequestHeader(value = "X-Forwarded-For", required = false) String forwardedFor,
                               @RequestHeader(value = "User-Agent", required = false) String userAgent) {
        TokenPair pair = authService.login(request.username(), request.password(), forwardedFor, userAgent);
        return TokenResponse.from(pair);
    }

    @PostMapping("/refresh")
    public TokenResponse refresh(@Valid @RequestBody RefreshRequest request,
                                 @RequestHeader(value = "X-Forwarded-For", required = false) String forwardedFor,
                                 @RequestHeader(value = "User-Agent", required = false) String userAgent) {
        TokenPair pair = authService.refresh(request.refreshToken(), forwardedFor, userAgent);
        return TokenResponse.from(pair);
    }

    @PostMapping("/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void logout(@RequestHeader("Authorization") String authorizationHeader) {
        if (authorizationHeader == null || !authorizationHeader.startsWith("Bearer ")) {
            throw new IllegalArgumentException("Authorization header is required");
        }
        authService.logout(authorizationHeader.substring(7));
    }

    @PostMapping("/logout-all")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void logoutAll() {
        authService.logoutAllCurrentUserSessions();
    }

    public record RegisterRequest(
            @NotBlank(message = "El nombre es obligatorio") String name,
            @NotBlank(message = "El username es obligatorio") String username,
            @NotBlank(message = "La password es obligatoria")
            @Size(min = 6, message = "La password debe tener al menos 6 caracteres")
            String password
    ) {
    }

    public record LoginRequest(
            @NotBlank(message = "El username es obligatorio") String username,
            @NotBlank(message = "La password es obligatoria") String password
    ) {
    }

    public record RefreshRequest(
            @NotBlank(message = "El refresh token es obligatorio") String refreshToken
    ) {
    }

    public record TokenResponse(
            String accessToken,
            String refreshToken,
            String tokenType,
            long accessExpiresAt,
            long refreshExpiresAt
    ) {
        static TokenResponse from(TokenPair pair) {
            return new TokenResponse(
                    pair.accessToken(),
                    pair.refreshToken(),
                    "Bearer",
                    pair.accessExpiresAt().getEpochSecond(),
                    pair.refreshExpiresAt().getEpochSecond()
            );
        }
    }
}
