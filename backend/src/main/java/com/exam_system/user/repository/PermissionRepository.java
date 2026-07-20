package com.exam_system.user.repository;

import com.exam_system.user.domain.Permission;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface PermissionRepository extends JpaRepository<Permission, Long> {
    Optional<Permission> findByCode(String code);

    List<Permission> findByCodeIn(Collection<String> codes);

    boolean existsByCode(String code);
}
