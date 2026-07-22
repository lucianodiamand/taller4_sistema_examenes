package com.exam_system.user.application;

import com.exam_system.user.domain.Permission;
import com.exam_system.user.domain.Role;
import com.exam_system.user.repository.PermissionRepository;
import com.exam_system.user.repository.RoleRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Service
public class RolePermissionService {

    private final RoleRepository roleRepository;
    private final PermissionRepository permissionRepository;

    public RolePermissionService(RoleRepository roleRepository, PermissionRepository permissionRepository) {
        this.roleRepository = roleRepository;
        this.permissionRepository = permissionRepository;
    }

    @Transactional(readOnly = true)
    public List<Role> findAllRoles() {
        return roleRepository.findAll();
    }

    @Transactional
    public Role updateRoleDescription(String roleName, String description) {
        Role role = roleRepository.findByName(roleName)
                .orElseThrow(() -> new EntityNotFoundException("Role not found"));
        role.setDescription(normalizeOptional(description));
        return roleRepository.save(role);
    }

    @Transactional(readOnly = true)
    public Set<String> getPermissions(String roleName) {
        Role role = roleRepository.findByName(roleName)
                .orElseThrow(() -> new EntityNotFoundException("Role not found"));
        return toPermissionCodes(role.getPermissions());
    }

    @Transactional
    public Set<String> replacePermissions(String roleName, List<String> permissionCodes) {
        Role role = roleRepository.findByName(roleName)
                .orElseThrow(() -> new EntityNotFoundException("Role not found"));
        List<Permission> permissions = permissionRepository.findByCodeIn(permissionCodes);
        if (permissions.size() != permissionCodes.size()) {
            throw new IllegalArgumentException("One or more permissions do not exist");
        }
        role.getPermissions().clear();
        role.getPermissions().addAll(permissions);
        roleRepository.save(role);
        return toPermissionCodes(role.getPermissions());
    }

    private Set<String> toPermissionCodes(Set<Permission> permissions) {
        Set<String> codes = new LinkedHashSet<>();
        for (Permission permission : permissions) {
            if (permission == null) {
                continue;
            }
            codes.add(permission.getCode());
        }
        return codes;
    }

    private String normalizeOptional(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}
