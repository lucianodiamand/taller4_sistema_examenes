package com.exam_system.user.application;

import com.exam_system.auth.security.CurrentUser;
import org.springframework.stereotype.Component;

@Component("userAccess")
public class UserAccess {

    private final CurrentUser currentUser;

    public UserAccess(CurrentUser currentUser) {
        this.currentUser = currentUser;
    }

    public boolean isSelf(Long userId) {
        return currentUser.id().equals(userId);
    }
}
