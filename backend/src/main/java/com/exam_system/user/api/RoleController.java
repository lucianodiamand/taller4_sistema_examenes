package com.exam_system.user.api;

import com.exam_system.user.application.RolePermissionService;
import com.exam_system.user.domain.Role;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Locale;
import java.util.List;
import java.util.Set;

@RestController
@RequestMapping("/api/roles")
public class RoleController {

    private final RolePermissionService rolePermissionService;

    public RoleController(RolePermissionService rolePermissionService) {
        this.rolePermissionService = rolePermissionService;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('permissions.manage')")
    public List<RoleResponse> findAll() {
        return rolePermissionService.findAllRoles().stream().map(RoleController::toResponse).toList();
    }

    @PatchMapping("/{role}")
    @PreAuthorize("hasAuthority('permissions.manage')")
    public RoleResponse updateRole(@PathVariable String role,
                                   @Valid @RequestBody UpdateRoleRequest request) {
        Role updated = rolePermissionService.updateRoleDescription(role.toUpperCase(Locale.ROOT), request.description());
        return toResponse(updated);
    }

    @GetMapping("/{role}/permissions")
    @PreAuthorize("hasAuthority('permissions.manage')")
    public RolePermissionsResponse getPermissions(@PathVariable String role) {
        String normalizedRole = role.toUpperCase(Locale.ROOT);
        return new RolePermissionsResponse(normalizedRole, rolePermissionService.getPermissions(normalizedRole));
    }

    @PatchMapping("/{role}/permissions")
    @PreAuthorize("hasAuthority('permissions.manage')")
    public RolePermissionsResponse replacePermissions(@PathVariable String role,
                                                      @Valid @RequestBody ReplaceRolePermissionsRequest request) {
        String normalizedRole = role.toUpperCase(Locale.ROOT);
        return new RolePermissionsResponse(
                normalizedRole,
                rolePermissionService.replacePermissions(normalizedRole, request.permissionCodes())
        );
    }

    private static RoleResponse toResponse(Role role) {
        return new RoleResponse(role.getId(), role.getName(), role.getDescription());
    }

    public record UpdateRoleRequest(String description) {
    }

    public record ReplaceRolePermissionsRequest(
            @NotNull(message = "permissionCodes is required")
            @NotEmpty(message = "permissionCodes must not be empty")
            List<String> permissionCodes
    ) {
    }

    public record RolePermissionsResponse(String role, Set<String> permissionCodes) {
    }

    public record RoleResponse(Long id, String name, String description) {
    }
}
