package com.exam_system.user.repository;

import com.exam_system.user.domain.Role;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface RoleRepository extends JpaRepository<Role, Long> {
    Optional<Role> findByName(String name);

    boolean existsByName(String name);

    boolean existsByPermissionsId(Long permissionId);
}
