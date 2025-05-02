package com.codejam.codex.authzen.configs;

import com.codejam.codex.authzen.models.Permission;
import com.codejam.codex.authzen.models.Role;
import com.codejam.codex.authzen.models.RolePermission;
import com.codejam.codex.authzen.repositories.PermissionRepository;
import com.codejam.codex.authzen.repositories.RolePermissionRepository;
import com.codejam.codex.authzen.repositories.RoleRepository;
import jakarta.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Arrays;
import java.util.List;

@Configuration
public class PermissionInitializer {

    private static final Logger logger = LoggerFactory.getLogger(PermissionInitializer.class);

    @Bean
    @Transactional
    public CommandLineRunner initializePermissions(
            PermissionRepository permissionRepository,
            RoleRepository roleRepository,
            RolePermissionRepository rolePermissionRepository
    ) {
        return args -> {
            logger.info("Initializing permissions...");

            List<Permission> defaultPermissions = getDefaultPermissions();
            List<Permission> userPermissions = getUserPermissions();

            for (Permission permission : defaultPermissions) {
                permissionRepository.findByName(permission.getName()).orElseGet(() -> {
                    logger.info("Creating permission: {}", permission.getName());
                    return permissionRepository.save(permission);
                });
            }

            // === ROLE_ADMIN setup ===
            Role adminRole = roleRepository.findByNameWithPermissions("ROLE_ADMIN")
                    .orElseThrow(() -> new IllegalStateException("ROLE_ADMIN not found"));

            logger.info("Assigning missing permissions to ROLE_ADMIN...");
            for (Permission permission : defaultPermissions) {
                if (adminRole.getRolePermissions().stream()
                        .noneMatch(rp -> rp.getPermission().getName().equals(permission.getName()))) {

                    RolePermission rp = RolePermission.builder()
                            .role(adminRole)
                            .permission(permissionRepository.findByName(permission.getName()).orElseThrow())
                            .build();
                    rolePermissionRepository.save(rp);
                    logger.info("Assigned '{}' to ROLE_ADMIN", permission.getName());
                }
            }

            // === ROLE_USER setup ===
            Role userRole = roleRepository.findByNameWithPermissions("ROLE_USER")
                    .orElseThrow(() -> new IllegalStateException("ROLE_USER not found"));

            logger.info("Assigning relevant permissions to ROLE_USER...");
            for (Permission permission : userPermissions) {
                if (userRole.getRolePermissions().stream()
                        .noneMatch(rp -> rp.getPermission().getName().equals(permission.getName()))) {

                    RolePermission rp = RolePermission.builder()
                            .role(userRole)
                            .permission(permissionRepository.findByName(permission.getName()).orElseThrow())
                            .build();
                    rolePermissionRepository.save(rp);
                    logger.info("Assigned '{}' to ROLE_USER", permission.getName());
                }
            }

            logger.info("Permission initialization complete.");
        };
    }

    private List<Permission> getDefaultPermissions() {
        return Arrays.asList(
                Permission.builder().name("CREATE_USER").description("Create new user").build(),
                Permission.builder().name("UPDATE_USER").description("Update user information").build(),
                Permission.builder().name("DELETE_USER").description("Delete a user").build(),
                Permission.builder().name("USER_LOGOUT").description("Logout user").build(),
                Permission.builder().name("ACTIVATE_USER").description("Activate user account").build(),
                Permission.builder().name("DEACTIVATE_USER").description("Deactivate user account").build(),
                Permission.builder().name("VIEW_USER").description("View user details").build(),
                Permission.builder().name("CREATE_ROLE").description("Create new role").build(),
                Permission.builder().name("UPDATE_ROLE").description("Update role").build(),
                Permission.builder().name("DELETE_ROLE").description("Delete a role").build(),
                Permission.builder().name("ASSIGN_ROLE").description("Assign role to user").build(),
                Permission.builder().name("VIEW_ROLE").description("View role details").build(),
                Permission.builder().name("CREATE_PERMISSION").description("Create new permission").build(),
                Permission.builder().name("UPDATE_PERMISSION").description("Update permission").build(),
                Permission.builder().name("DELETE_PERMISSION").description("Delete permission").build(),
                Permission.builder().name("ASSIGN_PERMISSION").description("Assign permission to role").build(),
                Permission.builder().name("VIEW_PERMISSION").description("View permission details").build(),
                Permission.builder().name("VIEW_AUDIT_LOG").description("View audit logs").build(),
                Permission.builder().name("EXPORT_AUDIT_LOG").description("Export audit logs").build(),
                Permission.builder().name("VIEW_DASHBOARD").description("Access dashboard").build(),
                Permission.builder().name("VIEW_STATS").description("View system statistics").build(),
                Permission.builder().name("CREATE_CONTENT").description("Create content").build(),
                Permission.builder().name("UPDATE_CONTENT").description("Update content").build(),
                Permission.builder().name("DELETE_CONTENT").description("Delete content").build(),
                Permission.builder().name("VIEW_CONTENT").description("View content").build(),
                Permission.builder().name("VIEW_ADMIN_PANEL").description("Access admin panel").build(),
                Permission.builder().name("CONFIGURE_SYSTEM").description("Configure system settings").build(),
                Permission.builder().name("VIEW_SYSTEM_STATUS").description("View system status").build()
        );
    }

    private List<Permission> getUserPermissions() {
        return Arrays.asList(
                Permission.builder().name("VIEW_USER").description("View user details").build(),
                Permission.builder().name("UPDATE_USER").description("Update user information").build(),
                Permission.builder().name("VIEW_DASHBOARD").description("Access dashboard").build(),
                Permission.builder().name("VIEW_STATS").description("View system statistics").build(),
                Permission.builder().name("VIEW_CONTENT").description("View content").build(),
                Permission.builder().name("USER_LOGOUT").description("Logout user").build()
                );
    }
}
