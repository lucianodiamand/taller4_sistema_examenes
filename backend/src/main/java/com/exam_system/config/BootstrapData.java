package com.exam_system.config;

import com.exam_system.user.domain.Permission;
import com.exam_system.user.domain.Role;
import com.exam_system.user.domain.User;
import com.exam_system.user.repository.PermissionRepository;
import com.exam_system.user.repository.RoleRepository;
import com.exam_system.user.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Configuration
public class BootstrapData {

    private static final Logger logger = LoggerFactory.getLogger(BootstrapData.class);

    @Bean
    public ApplicationRunner adminSeeder(BootstrapProperties bootstrapProperties,
                                         UserRepository userRepository,
                                         RoleRepository roleRepository,
                                         PermissionRepository permissionRepository,
                                         PasswordEncoder passwordEncoder) {
        return args -> {
            seedRolesAndPermissions(roleRepository, permissionRepository);

            if (!bootstrapProperties.isSeedAdmin()) {
                return;
            }

            if (userRepository.existsByUsername(bootstrapProperties.getAdminUsername())) {
                return;
            }

            Role adminRole = roleRepository.findByName("ADMIN")
                    .orElseThrow(() -> new IllegalStateException("ADMIN role not found"));

            User admin = new User();
            admin.setName(bootstrapProperties.getAdminName());
            admin.setUsername(bootstrapProperties.getAdminUsername());
            admin.setPassword(passwordEncoder.encode(bootstrapProperties.getAdminPassword()));
            admin.setRole(adminRole);
            userRepository.save(admin);

            logger.info("Seeded default admin user {}", bootstrapProperties.getAdminUsername());
        };
    }

    private void seedRolesAndPermissions(RoleRepository roleRepository, PermissionRepository permissionRepository) {
        Role admin = ensureRole(roleRepository, "ADMIN", "System administrator");
        Role professor = ensureRole(roleRepository, "PROFESSOR", "Professor");
        Role student = ensureRole(roleRepository, "STUDENT", "Student");

        Map<String, String> permissionCatalog = new LinkedHashMap<>();
        permissionCatalog.put("permissions.manage", "Manage role permissions");
        permissionCatalog.put("users.read.any", "Read any user profile");
        permissionCatalog.put("users.read.self", "Read own profile");
        permissionCatalog.put("users.update.self", "Update own profile");
        permissionCatalog.put("users.create.professor", "Create professor users");
        permissionCatalog.put("exams.create", "Create exams");
        permissionCatalog.put("exams.solve", "Solve exams");
        permissionCatalog.put("exams.grade", "Grade exams");
        permissionCatalog.put("exam.validations.read.self", "Read own validation comments");
        permissionCatalog.put("exam.results.read.self", "Read own exam results");

        for (Map.Entry<String, String> entry : permissionCatalog.entrySet()) {
            permissionRepository.findByCode(entry.getKey()).orElseGet(() -> {
                Permission permission = new Permission();
                permission.setCode(entry.getKey());
                permission.setDescription(entry.getValue());
                return permissionRepository.save(permission);
            });
        }

        seedRolePermissionsIfMissing(permissionRepository, roleRepository, admin, List.of(
                "permissions.manage",
                "users.read.any",
                "users.read.self",
                "users.update.self",
                "users.create.professor",
                "exams.create",
                "exams.solve",
                "exams.grade"
        ));

        seedRolePermissionsIfMissing(permissionRepository, roleRepository, professor, List.of(
                "users.read.self",
                "users.update.self",
                "exams.create",
                "exams.grade"
        ));

        seedRolePermissionsIfMissing(permissionRepository, roleRepository, student, List.of(
                "users.read.self",
                "users.update.self",
                "exams.solve",
                "exam.validations.read.self",
                "exam.results.read.self"
        ));
    }

    private Role ensureRole(RoleRepository roleRepository, String name, String description) {
        return roleRepository.findByName(name).orElseGet(() -> {
            Role role = new Role();
            role.setName(name);
            role.setDescription(description);
            return roleRepository.save(role);
        });
    }

    private void seedRolePermissionsIfMissing(PermissionRepository permissionRepository,
                                              RoleRepository roleRepository,
                                              Role role,
                                              List<String> permissionCodes) {
        if (!role.getPermissions().isEmpty()) {
            return;
        }

        Set<Permission> permissions = Set.copyOf(permissionRepository.findByCodeIn(permissionCodes));
        role.getPermissions().addAll(permissions);
        roleRepository.save(role);
    }
}
