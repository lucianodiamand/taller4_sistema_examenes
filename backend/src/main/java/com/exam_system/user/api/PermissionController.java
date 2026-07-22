package com.exam_system.user.api;

import com.exam_system.user.application.PermissionService;
import com.exam_system.user.domain.Permission;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
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
@RequestMapping("/api/permissions")
public class PermissionController {

    private final PermissionService permissionService;

    public PermissionController(PermissionService permissionService) {
        this.permissionService = permissionService;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('permissions.manage')")
    public List<PermissionResponse> findAll() {
        return permissionService.findAll().stream().map(PermissionController::toResponse).toList();
    }

    @PostMapping
    @PreAuthorize("hasAuthority('permissions.manage')")
    @ResponseStatus(HttpStatus.CREATED)
    public PermissionResponse create(@Valid @RequestBody CreatePermissionRequest request) {
        return toResponse(permissionService.create(request.code(), request.description()));
    }

    @PatchMapping("/{id}")
    @PreAuthorize("hasAuthority('permissions.manage')")
    public PermissionResponse update(@PathVariable Long id, @Valid @RequestBody UpdatePermissionRequest request) {
        return toResponse(permissionService.update(id, request.code(), request.description()));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('permissions.manage')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        permissionService.delete(id);
    }

    private static PermissionResponse toResponse(Permission permission) {
        return new PermissionResponse(permission.getId(), permission.getCode(), permission.getDescription());
    }

    public record CreatePermissionRequest(
            @NotBlank(message = "Code is required") String code,
            String description
    ) {
    }

    public record UpdatePermissionRequest(
            @NotBlank(message = "Code is required") String code,
            String description
    ) {
    }

    public record PermissionResponse(Long id, String code, String description) {
    }
}
