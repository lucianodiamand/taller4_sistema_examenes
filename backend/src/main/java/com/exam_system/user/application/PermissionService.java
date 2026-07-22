package com.exam_system.user.application;

import com.exam_system.user.domain.Permission;
import com.exam_system.user.repository.RoleRepository;
import com.exam_system.user.repository.PermissionRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class PermissionService {

    private final PermissionRepository permissionRepository;
    private final RoleRepository roleRepository;

    public PermissionService(PermissionRepository permissionRepository, RoleRepository roleRepository) {
        this.permissionRepository = permissionRepository;
        this.roleRepository = roleRepository;
    }

    @Transactional(readOnly = true)
    public List<Permission> findAll() {
        return permissionRepository.findAll();
    }

    @Transactional
    public Permission create(String code, String description) {
        String normalizedCode = normalizeRequired(code, "code");
        if (permissionRepository.existsByCode(normalizedCode)) {
            throw new IllegalArgumentException("Permission code already exists");
        }

        Permission permission = new Permission();
        permission.setCode(normalizedCode);
        permission.setDescription(normalizeOptional(description));
        return permissionRepository.save(permission);
    }

    @Transactional
    public Permission update(Long id, String code, String description) {
        Permission permission = permissionRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Permission not found"));

        String normalizedCode = normalizeRequired(code, "code");
        permissionRepository.findByCode(normalizedCode)
                .filter(existing -> !existing.getId().equals(id))
                .ifPresent(existing -> {
                    throw new IllegalArgumentException("Permission code already exists");
                });

        permission.setCode(normalizedCode);
        permission.setDescription(normalizeOptional(description));
        return permissionRepository.save(permission);
    }

    @Transactional
    public void delete(Long id) {
        Permission permission = permissionRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Permission not found"));

        if (roleRepository.existsByPermissionsId(id)) {
            throw new IllegalArgumentException("Cannot delete permission assigned to roles");
        }

        permissionRepository.delete(permission);
    }

    private String normalizeRequired(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " is required");
        }
        return value.trim();
    }

    private String normalizeOptional(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}
