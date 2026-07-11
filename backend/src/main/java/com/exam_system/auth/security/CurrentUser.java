package com.exam_system.auth.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component
public class CurrentUser {

    public Long id() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getDetails() instanceof AuthenticatedUser user)) {
            throw new IllegalStateException("No authenticated user");
        }
        return user.getId();
    }

    public String username() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getDetails() instanceof AuthenticatedUser user)) {
            throw new IllegalStateException("No authenticated user");
        }
        return user.getUsername();
    }

    public String role() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getDetails() instanceof AuthenticatedUser user)) {
            throw new IllegalStateException("No authenticated user");
        }
        return user.getRole();
    }
}
