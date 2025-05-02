package com.codejam.codex.authzen.endpoint;

import com.codejam.codex.authzen.dtos.inputs.DelegateRequest;
import com.codejam.codex.authzen.dtos.inputs.RoleRequest;
import com.codejam.codex.authzen.dtos.inputs.RoleUpdateRequest;
import com.codejam.codex.authzen.dtos.outputs.AuditLogResponse;
import com.codejam.codex.authzen.dtos.outputs.UpdateUserResponse;
import com.codejam.codex.authzen.dtos.outputs.UserResponse;
import com.codejam.codex.authzen.models.AuditLog;
import com.codejam.codex.authzen.models.User;
import com.codejam.codex.authzen.services.AdminService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Endpoint for handling admin-related operations such as managing users,
 * roles, audit logs, and permission delegation.
 */
@Component
public class AdminEndpoint {

    private final AdminService adminService;

    @Autowired
    public AdminEndpoint(AdminService adminService) {
        this.adminService = adminService;
    }

    /**
     * Retrieves all users in the system.
     *
     * @param adminUsername The username of the requesting admin.
     * @return A list of user responses.
     */
    public List<UserResponse> getAllUsers(String adminUsername) {
        return adminService.getAllUsers(adminUsername);
    }

    /**
     * Retrieves a user by their ID.
     *
     * @param userId The ID of the user to retrieve.
     * @return The User object.
     */
    public UserResponse getUserById(Long userId) {
        return adminService.getUserById(userId);
    }

    /**
     * Updates the roles of a specific user.
     *
     * @param userId        The ID of the user to update.
     * @param request       The role update request containing new role info.
     * @param adminUsername The username of the requesting admin.
     * @return A message indicating the result of the operation.
     */
    public UpdateUserResponse updateUserRoles(Long userId, RoleUpdateRequest request, String adminUsername) {
        return adminService.updateUserRoles(userId, request, adminUsername);
    }

    /**
     * Creates a new role in the system.
     *
     * @param request       The role creation request.
     * @param adminUsername The username of the requesting admin.
     * @return A message indicating the result of the operation.
     */
    public String createRole(RoleRequest request, String adminUsername) {
        return adminService.createRole(request, adminUsername);
    }

    /**
     * Retrieves the audit logs of the system.
     *
     * @param adminUsername The username of the requesting admin.
     * @return A list of audit logs.
     */
    public List<AuditLogResponse> getAuditLogs(String adminUsername) {
        return adminService.getAuditLogs(adminUsername);
    }

    /**
     * Delegates specific permissions to another user.
     *
     * @param request       The delegation request containing target user and permissions.
     * @param adminUsername The username of the requesting admin.
     * @return A message indicating the result of the operation.
     */
    public String delegatePermissions(DelegateRequest request, String adminUsername) {
        return adminService.delegatePermissions(request, adminUsername);
    }
}