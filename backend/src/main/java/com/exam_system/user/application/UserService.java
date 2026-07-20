package com.exam_system.user.application;

import com.exam_system.auth.security.CurrentUser;
import com.exam_system.user.domain.Role;
import com.exam_system.user.domain.User;
import com.exam_system.user.repository.RoleRepository;
import com.exam_system.user.repository.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;
import java.util.List;

@Service
public class UserService {

    private static final String ROLE_ADMIN = "ADMIN";
    private static final String ROLE_PROFESSOR = "PROFESSOR";
    private static final String ROLE_STUDENT = "STUDENT";

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final CurrentUser currentUser;

    public UserService(UserRepository userRepository,
                       RoleRepository roleRepository,
                       PasswordEncoder passwordEncoder,
                       CurrentUser currentUser) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
        this.currentUser = currentUser;
    }

    @Transactional(readOnly = true)
    public List<User> findAll() {
        return userRepository.findAll();
    }

    @Transactional(readOnly = true)
    public User findById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("User not found"));
    }

    @Transactional(readOnly = true)
    public User findMe() {
        return findById(currentUser.id());
    }

    @Transactional
    public User updateMe(String name, String password) {
        User user = userRepository.findById(currentUser.id())
                .orElseThrow(() -> new EntityNotFoundException("User not found"));
        if (name != null && !name.isBlank()) {
            user.setName(name);
        }
        if (password != null && !password.isBlank()) {
            user.setPassword(passwordEncoder.encode(password));
        }
        return userRepository.save(user);
    }

    @Transactional
    public User createProfessor(String name, String username, String password) {
        return createByAdmin(name, username, password, ROLE_PROFESSOR);
    }

    @Transactional
    public User createByAdmin(String name, String username, String password, String roleName) {
        String normalizedUsername = normalizeRequired(username, "username");
        if (userRepository.existsByUsername(normalizedUsername)) {
            throw new IllegalArgumentException("Username already exists");
        }

        Role role = resolveAllowedManagedRole(roleName);

        User user = new User();
        user.setName(normalizeRequired(name, "name"));
        user.setUsername(normalizedUsername);
        user.setPassword(passwordEncoder.encode(normalizeRequired(password, "password")));
        user.setRole(role);
        return userRepository.save(user);
    }

    @Transactional
    public User updateByAdmin(Long id, String name, String password, String roleName) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("User not found"));

        denyIfAdminTarget(user);

        if (name != null) {
            user.setName(normalizeRequired(name, "name"));
        }
        if (password != null) {
            user.setPassword(passwordEncoder.encode(normalizeRequired(password, "password")));
        }
        if (roleName != null) {
            user.setRole(resolveAllowedManagedRole(roleName));
        }

        return userRepository.save(user);
    }

    @Transactional
    public void deleteByAdmin(Long id) {
        if (currentUser.id().equals(id)) {
            throw new IllegalArgumentException("Cannot delete current user");
        }

        User user = userRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("User not found"));

        denyIfAdminTarget(user);
        userRepository.delete(user);
    }

    private Role resolveAllowedManagedRole(String roleName) {
        String normalized = normalizeRequired(roleName, "role").toUpperCase(Locale.ROOT);
        if (!ROLE_PROFESSOR.equals(normalized) && !ROLE_STUDENT.equals(normalized)) {
            throw new IllegalArgumentException("Role not allowed");
        }

        return roleRepository.findByName(normalized)
                .orElseThrow(() -> new EntityNotFoundException("Role " + normalized + " not found"));
    }

    private void denyIfAdminTarget(User user) {
        if (user.getRole() != null && ROLE_ADMIN.equals(user.getRole().getName())) {
            throw new IllegalArgumentException("Cannot modify admin users");
        }
    }

    private String normalizeRequired(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " is required");
        }
        return value.trim();
    }
}
