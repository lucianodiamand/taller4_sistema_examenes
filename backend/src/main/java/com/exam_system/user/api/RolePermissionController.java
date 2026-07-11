package com.exam_system.user.api;

import com.exam_system.user.application.RolePermissionService;
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

import java.util.List;
import java.util.Set;

@RestController
@RequestMapping("/api/roles")
public class RolePermissionController {

    private final RolePermissionService rolePermissionService;

    public RolePermissionController(RolePermissionService rolePermissionService) {
        this.rolePermissionService = rolePermissionService;
    }

    @GetMapping("/{role}/permissions")
    @PreAuthorize("hasAuthority('permissions.manage')")
    public RolePermissionsResponse getPermissions(@PathVariable String role) {
        return new RolePermissionsResponse(role.toUpperCase(), rolePermissionService.getPermissions(role.toUpperCase()));
    }

    @PatchMapping("/{role}/permissions")
    @PreAuthorize("hasAuthority('permissions.manage')")
    public RolePermissionsResponse replacePermissions(@PathVariable String role,
                                                      @Valid @RequestBody ReplaceRolePermissionsRequest request) {
        return new RolePermissionsResponse(
                role.toUpperCase(),
                rolePermissionService.replacePermissions(role.toUpperCase(), request.permissionCodes())
        );
    }

    public record ReplaceRolePermissionsRequest(
            @NotNull(message = "permissionCodes is required")
            @NotEmpty(message = "permissionCodes must not be empty")
            List<String> permissionCodes
    ) {
    }

    public record RolePermissionsResponse(String role, Set<String> permissionCodes) {
    }
}
