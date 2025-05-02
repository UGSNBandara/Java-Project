package com.codejam.codex.authzen.configs;

import com.codejam.codex.authzen.models.Role;
import com.codejam.codex.authzen.models.User;
import com.codejam.codex.authzen.models.UserRole;
import com.codejam.codex.authzen.repositories.RoleRepository;
import com.codejam.codex.authzen.repositories.UserRepository;
import com.codejam.codex.authzen.repositories.UserRoleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.sql.Timestamp;
import java.util.List;

@Configuration
@RequiredArgsConstructor
public class RoleInitializer {

    private final RoleRepository roleRepository;
    private final UserRepository userRepository;
    private final UserRoleRepository userRoleRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${admin.email}")
    private String adminEmail;

    @Value("${admin.username}")
    private String adminUsername;

    @Value("${admin.password}")
    private String adminPassword;

    @Bean
    CommandLineRunner initializeRolesAndAdmin() {
        return args -> {
            Role userRole = createRoleIfNotExists("ROLE_USER", "Default user role");
            Role adminRole = createRoleIfNotExists("ROLE_ADMIN", "Administrator with full access");

            createAdminUserIfNotExists(adminRole);
        };
    }

    private Role createRoleIfNotExists(String name, String description) {
        List<Role> roles = roleRepository.findByName(name);

        if (roles.isEmpty()) {
            Role newRole = Role.builder()
                    .name(name)
                    .description(description)
                    .build();
            return roleRepository.save(newRole);
        }

        return roles.get(0);
    }

    private void createAdminUserIfNotExists(Role adminRole) {
        if (userRepository.findByEmail(adminEmail).isEmpty()) {
            User admin = User.builder()
                    .username(adminUsername)
                    .email(adminEmail)
                    .password(passwordEncoder.encode(adminPassword))
                    .isActive(true)
                    .isLocked(false)
                    .createdAt(new Timestamp(System.currentTimeMillis()))
                    .build();

            User savedAdmin = userRepository.save(admin);

            UserRole userRole = new UserRole();
            userRole.setUser(savedAdmin);
            userRole.setRole(adminRole);
            userRoleRepository.save(userRole);
        }
    }
}
