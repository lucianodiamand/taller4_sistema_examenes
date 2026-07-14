package com.exam_system.user.api;

import com.exam_system.user.application.UserService;
import com.exam_system.user.domain.User;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
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

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('users.read.any')")
    public List<UserResponse> findAll() {
        return userService.findAll().stream().map(UserController::toResponse).toList();
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('users.read.any') or @userAccess.isSelf(#id)")
    public UserResponse findById(@PathVariable Long id) {
        return toResponse(userService.findById(id));
    }

    @GetMapping("/me")
    @PreAuthorize("hasAuthority('users.read.self')")
    public UserResponse findMe() {
        return toResponse(userService.findMe());
    }

    @PatchMapping("/me")
    @PreAuthorize("hasAuthority('users.update.self')")
    public UserResponse updateMe(@RequestBody UpdateMeRequest request) {
        return toResponse(userService.updateMe(request.name(), request.password()));
    }

    @PostMapping("/professors")
    @PreAuthorize("hasAuthority('users.create.professor')")
    @ResponseStatus(HttpStatus.CREATED)
    public UserResponse createProfessor(@Valid @RequestBody CreateProfessorRequest request) {
        return toResponse(userService.createProfessor(request.name(), request.username(), request.password()));
    }

    private static UserResponse toResponse(User user) {
        return new UserResponse(user.getId(), user.getName(), user.getUsername(), user.getRole().getName());
    }

    public record UpdateMeRequest(String name, String password) {
    }

    public record CreateProfessorRequest(
            @NotBlank(message = "El nombre es obligatorio") String name,
            @NotBlank(message = "El username es obligatorio") String username,
            @NotBlank(message = "La password es obligatoria")
            @Size(min = 6, message = "La password debe tener al menos 6 caracteres")
            String password
    ) {
    }

    public record UserResponse(Long id, String name, String username, String role) {
    }
}
