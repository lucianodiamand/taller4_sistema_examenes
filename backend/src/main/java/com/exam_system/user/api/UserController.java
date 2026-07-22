package com.exam_system.user.api;

import com.exam_system.auth.security.CurrentUser;
import com.exam_system.user.application.UserService;
import com.exam_system.user.domain.User;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;
    private final CurrentUser currentUser;

    public UserController(UserService userService, CurrentUser currentUser) {
        this.userService = userService;
        this.currentUser = currentUser;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('users.read.any')")
    public List<UserResponse> findAll() {
        Long currentId = currentUser.id();
        return userService.findAll().stream().map(user -> toResponse(user, currentId)).toList();
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('users.read.any') or @userAccess.isSelf(#id)")
    public UserResponse findById(@PathVariable Long id) {
        return toResponse(userService.findById(id), currentUser.id());
    }

    @GetMapping("/me")
    @PreAuthorize("hasAuthority('users.read.self')")
    public UserResponse findMe() {
        Long currentId = currentUser.id();
        return toResponse(userService.findMe(), currentId);
    }

    @PatchMapping("/me")
    @PreAuthorize("hasAuthority('users.update.self')")
    public UserResponse updateMe(@RequestBody UpdateMeRequest request) {
        Long currentId = currentUser.id();
        return toResponse(userService.updateMe(request.name(), request.password()), currentId);
    }

    @PostMapping("/professors")
    @PreAuthorize("hasAuthority('users.create.professor')")
    @ResponseStatus(HttpStatus.CREATED)
    public UserResponse createProfessor(@Valid @RequestBody CreateProfessorRequest request) {
        Long currentId = currentUser.id();
        return toResponse(userService.createProfessor(request.name(), request.username(), request.password()), currentId);
    }

    @PostMapping
    @PreAuthorize("hasAuthority('users.create.any')")
    @ResponseStatus(HttpStatus.CREATED)
    public UserResponse create(@Valid @RequestBody CreateUserRequest request) {
        Long currentId = currentUser.id();
        return toResponse(
                userService.createByAdmin(request.name(), request.username(), request.password(), request.role()),
                currentId
        );
    }

    @PatchMapping("/{id}")
    @PreAuthorize("hasAuthority('users.update.any')")
    public UserResponse update(@PathVariable Long id, @RequestBody UpdateUserRequest request) {
        Long currentId = currentUser.id();
        return toResponse(
                userService.updateByAdmin(id, request.name(), request.password(), request.role()),
                currentId
        );
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('users.delete.any')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        userService.deleteByAdmin(id);
    }

    private static UserResponse toResponse(User user, Long currentUserId) {
        String roleName = user.getRole() == null ? null : user.getRole().getName();
        boolean self = currentUserId != null && user.getId().equals(currentUserId);
        boolean manageable = !self && !"ADMIN".equals(roleName);
        return new UserResponse(user.getId(), user.getName(), user.getUsername(), roleName, self, manageable);
    }

    public record UpdateMeRequest(String name, String password) {
    }

    public record CreateProfessorRequest(
            @NotBlank(message = "Name is required") String name,
            @NotBlank(message = "Username is required") String username,
            @NotBlank(message = "Password is required")
            @Size(min = 6, message = "Password must be at least 6 characters")
            String password
    ) {
    }

    public record CreateUserRequest(
            @NotBlank(message = "Name is required") String name,
            @NotBlank(message = "Username is required") String username,
            @NotBlank(message = "Password is required")
            @Size(min = 6, message = "Password must be at least 6 characters")
            String password,
            @NotBlank(message = "Role is required")
            String role
    ) {
    }

    public record UpdateUserRequest(String name, String password, String role) {
    }

    public record UserResponse(Long id, String name, String username, String role, boolean self, boolean manageable) {
    }
}
