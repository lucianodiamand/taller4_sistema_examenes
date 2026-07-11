package com.exam_system.auth.security;

import com.exam_system.auth.application.JwtService;
import com.exam_system.auth.repository.AuthTokenRepository;
import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final UserDetailsService userDetailsService;
    private final AuthTokenRepository authTokenRepository;

    public JwtAuthenticationFilter(JwtService jwtService,
                                   UserDetailsService userDetailsService,
                                   AuthTokenRepository authTokenRepository) {
        this.jwtService = jwtService;
        this.userDetailsService = userDetailsService;
        this.authTokenRepository = authTokenRepository;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        String token = authHeader.substring(7);
        Claims claims;
        try {
            claims = jwtService.parseClaims(token);
        } catch (IllegalArgumentException ex) {
            filterChain.doFilter(request, response);
            return;
        }

        String tokenType = claims.get("type", String.class);
        if (!"access".equals(tokenType)) {
            filterChain.doFilter(request, response);
            return;
        }

        String jti = claims.get("jti", String.class);
        if (jti == null) {
            filterChain.doFilter(request, response);
            return;
        }

        boolean validDbToken = authTokenRepository.findByAccessJti(jti)
                .filter(t -> t.getRevokedAt() == null)
                .isPresent();

        if (!validDbToken) {
            filterChain.doFilter(request, response);
            return;
        }

        String username = claims.get("username", String.class);
        if (username == null || SecurityContextHolder.getContext().getAuthentication() != null) {
            filterChain.doFilter(request, response);
            return;
        }

        UserDetails userDetails = userDetailsService.loadUserByUsername(username);
        UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                userDetails,
                null,
                userDetails.getAuthorities()
        );
        authentication.setDetails(new AuthenticatedUser(
                Long.valueOf(claims.getSubject()),
                username,
                claims.get("role", String.class)
        ));
        SecurityContextHolder.getContext().setAuthentication(authentication);

        filterChain.doFilter(request, response);
    }
}
