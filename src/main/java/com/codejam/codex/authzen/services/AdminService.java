package com.codejam.codex.authzen.services;

import com.codejam.codex.authzen.dtos.inputs.DelegateRequest;
import com.codejam.codex.authzen.dtos.inputs.RoleRequest;
import com.codejam.codex.authzen.dtos.inputs.RoleUpdateRequest;
import com.codejam.codex.authzen.dtos.outputs.AuditLogResponse;
import com.codejam.codex.authzen.dtos.outputs.UpdateUserResponse;
import com.codejam.codex.authzen.dtos.outputs.UserResponse;
import com.codejam.codex.authzen.models.AuditLog;
import com.codejam.codex.authzen.models.Role;
import com.codejam.codex.authzen.models.User;
import com.codejam.codex.authzen.models.UserRole;
import com.codejam.codex.authzen.repositories.AuditLogRepository;
import com.codejam.codex.authzen.repositories.RoleRepository;
import com.codejam.codex.authzen.repositories.UserRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AdminService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final AuditLogRepository auditLogRepository;

    public List<UserResponse> getAllUsers(String adminUsername) {
        logAction(adminUsername, "User list got successfully");

        return userRepository.findAll()
                .stream()
                .map(user -> {
                    List<String> permissionNames = userRepository.findPermissionNamesByUsername(user.getUsername());
                    return UserResponse.fromEntity(user, permissionNames);
                })
                .toList();
    }

    @Transactional
    public UpdateUserResponse updateUserRoles(Long userId, RoleUpdateRequest request, String adminUsername) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found with ID: " + userId));

        // Validate requested roles
        List<Role> requestedRoles = new ArrayList<>();
        for (String roleName : request.getRoles()) {
            Role role = roleRepository.findByName(roleName)
                    .orElseThrow(() -> new RuntimeException("Role not found: " + roleName));
            requestedRoles.add(role);
        }

        // Update user roles
        user.getUserRoles().clear();
        for (Role role : requestedRoles) {
            UserRole userRole = UserRole.builder()
                    .user(user)
                    .role(role)
                    .build();
            user.getUserRoles().add(userRole);
        }

        user = userRepository.save(user);
        
        // Log the action
        logAction(adminUsername, "Updated roles for user: " + user.getUsername() + 
                " to: " + String.join(", ", request.getRoles()), null);
        
        return UpdateUserResponse.fromEntity(user);
    }

    public List<AuditLogResponse> getAuditLogs(String adminUsername) {
        List<AuditLog> auditLogs = auditLogRepository.findAll();

        return auditLogs.stream()
                .map(log -> {
                    return AuditLogResponse.builder()
                            .id(log.getId())
                            .username(log.getUser().getUsername())
                            .actionType(log.getActionType())
                            .ipAddress(log.getIpAddress())
                            .timestamp(log.getTimestamp())
                            .build();
                })
                .toList();
    }

    @Transactional
    public String createRole(RoleRequest request, String adminUsername) {
        // Validate role name
        if (roleRepository.existsByName(request.getName())) {
            throw new IllegalArgumentException("Role already exists: " + request.getName());
        }

        // Create new role
        Role role = Role.builder()
                .name(request.getName())
                .description(request.getDescription())
                .build();

        roleRepository.save(role);
        
        // Log the action
        logAction(adminUsername, "Created new role: " + request.getName(), null);
        
        return "Role created successfully: " + request.getName();
    }

    @Transactional
    public String delegatePermissions(DelegateRequest request, String adminUsername) {
        // Get target user
        User targetUser = userRepository.findById(request.getUserId())
                .orElseThrow(() -> new RuntimeException("User not found with ID: " + request.getUserId()));

        // Get role
        Role role = roleRepository.findByName(request.getRoleName())
                .orElseThrow(() -> new RuntimeException("Role not found: " + request.getRoleName()));

        // Check if user already has this role
        boolean alreadyAssigned = targetUser.getUserRoles().stream()
                .anyMatch(userRole -> userRole.getRole().getName().equals(request.getRoleName()));
        
        if (alreadyAssigned) {
            return "User already has this role: " + request.getRoleName();
        }

        // Create and save user role
        UserRole userRole = UserRole.builder()
                .user(targetUser)
                .role(role)
                .build();

        targetUser.getUserRoles().add(userRole);
        userRepository.save(targetUser);
        
        // Log the action
        logAction(adminUsername, "Delegated role: " + request.getRoleName() + 
                " to user: " + targetUser.getUsername(), null);
        
        return "Role delegated successfully: " + request.getRoleName();
    }

    public UserResponse getUserById(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found with ID: " + userId));
        List<String> permissionNames = userRepository.findPermissionNamesByUsername(user.getUsername());
        return UserResponse.fromEntity(user, permissionNames);
    }

    @Transactional
    public String lockUserAccount(Long userId, String adminUsername) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found with ID: " + userId));

        if (user.isAccountLocked()) {
            return "Account is already locked";
        }

        user.setAccountLocked(true);
        user.setLockReason("Manually locked by admin");
        user.setLockTimestamp(new Timestamp(System.currentTimeMillis()));
        userRepository.save(user);

        // Log the action
        logAction(adminUsername, "Locked user account: " + user.getUsername(), null);
        
        return "User account locked successfully";
    }

    @Transactional
    public String unlockUserAccount(Long userId, String adminUsername) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found with ID: " + userId));

        if (!user.isAccountLocked()) {
            return "Account is not locked";
        }

        user.setAccountLocked(false);
        user.setLockReason(null);
        user.setLockTimestamp(null);
        userRepository.save(user);

        // Log the action
        logAction(adminUsername, "Unlocked user account: " + user.getUsername(), null);
        
        return "User account unlocked successfully";
    }

    private void logAction(String adminUsername, String actionType, HttpServletRequest request) {
        // Get the actual user
        User adminUser = userRepository.findByUsername(adminUsername)
                .orElseThrow(() -> new RuntimeException("Admin user not found: " + adminUsername));

        // Create audit log
        AuditLog auditLog = AuditLog.builder()
                .user(adminUser)
                .actionType(actionType)
                .ipAddress(request != null ? request.getRemoteAddr() : "Unknown")
                .timestamp(new Timestamp(System.currentTimeMillis()))
                .build();

        auditLogRepository.save(auditLog);
    }
}
