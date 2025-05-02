package com.codejam.codex.authzen.controllers;

import com.codejam.codex.authzen.constants.ApiEndpoint;
import com.codejam.codex.authzen.dtos.inputs.DelegateRequest;
import com.codejam.codex.authzen.dtos.inputs.RoleRequest;
import com.codejam.codex.authzen.dtos.inputs.RoleUpdateRequest;
import com.codejam.codex.authzen.dtos.outputs.AuditLogResponse;
import com.codejam.codex.authzen.dtos.outputs.UpdateUserResponse;
import com.codejam.codex.authzen.dtos.outputs.UserResponse;
import com.codejam.codex.authzen.endpoint.AdminEndpoint;
import com.codejam.codex.authzen.endpoint.AuthEndpoint;
import com.codejam.codex.authzen.responses.AuthzenResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.annotation.Secured;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * AdminController handles all endpoints related to administrative actions,
 * such as user management, role assignments, audit log access, and permission delegation.
 * Access to all methods is restricted to users with the ADMIN role.
 */
@RestController
@RequestMapping(ApiEndpoint.ADMIN)
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    private final AdminEndpoint adminEndpoint;
    private final AuthEndpoint authEndpoint;

    @Autowired
    public AdminController(AdminEndpoint adminEndpoint, AuthEndpoint authEndpoint) {
        this.adminEndpoint = adminEndpoint;
        this.authEndpoint = authEndpoint;
    }
    /**
     * Helper method that checks if the current request is from an authenticated admin.
     * If valid, returns the username wrapped in 200 OK; otherwise returns 401/403 with a message.
     *
     * @param request HttpServletRequest containing the token
     * @return ResponseEntity with username in body if valid, or error message if unauthorized/forbidden
     */
    private String verifyAdmin(HttpServletRequest request) {
        String username = authEndpoint.getUsername(request);
        if (username == null || !authEndpoint.isAuthenticated(request)) {
            throw new AccessDeniedException("Unauthorized: No token provided.");
        }

        UserResponse userResponse = authEndpoint.getUserDetails(username);
        if (userResponse == null || !userResponse.getRoles().contains("ROLE_ADMIN")) {
            throw new AccessDeniedException("Forbidden: Insufficient permissions.");
        }

        return username;
    }

    /**
     * Retrieves a list of all registered users in the system.
     * Accessible only by authenticated admins.
     *
     * @param request HttpServletRequest containing authentication token
     * @return List of UserResponse objects
     */
    @GetMapping(ApiEndpoint.ADMIN_ALL_USERS)
    @Secured("ROLE_ADMIN")
    @PreAuthorize("hasAuthority('VIEW_USER')")
    public ResponseEntity<AuthzenResponse<List<UserResponse>>> getAllUsers(HttpServletRequest request) {
        String username = verifyAdmin(request);
        List<UserResponse> users = adminEndpoint.getAllUsers(username);
        AuthzenResponse<List<UserResponse>> response = new AuthzenResponse<>(users);
        response.setMessage("Users listed successfully.");
        return ResponseEntity.ok(response);
    }

    /**
     * Retrieves detailed information about a specific user by their ID.
     *
     * @param userId  ID of the target user
     * @param request HttpServletRequest with token
     * @return User object containing detailed user info
     */
    @GetMapping(ApiEndpoint.ADMIN_USERS)
    @Secured("ROLE_ADMIN")
    @PreAuthorize("hasAuthority('VIEW_USER')")
    public ResponseEntity<AuthzenResponse<UserResponse>> getUserDetails(@PathVariable("id") Long userId, HttpServletRequest request) {
        UserResponse userResponse = adminEndpoint.getUserById(userId);
        AuthzenResponse<UserResponse> response = new AuthzenResponse<>(userResponse);
        response.setMessage("User details retrieved successfully.");
        return ResponseEntity.ok(response);
    }

    /**
     * Updates the roles of a given user.
     * Ensures the request is made by a verified admin.
     *
     * @param userId            ID of the user whose roles will be updated
     * @param roleUpdateRequest Contains the updated list of roles
     * @param request           Authenticated request
     * @return Status message
     */
    @PutMapping(ApiEndpoint.ADMIN_USER_ROLES)
    @Secured("ROLE_ADMIN")
    @PreAuthorize("hasAuthority('UPDATE_USER')")
    public ResponseEntity<AuthzenResponse<UpdateUserResponse>> updateUserRole(@PathVariable("id") Long userId,
                                                                              @RequestBody RoleUpdateRequest roleUpdateRequest,
                                                                              HttpServletRequest request) {
        String username = verifyAdmin(request);
        UpdateUserResponse updated = adminEndpoint.updateUserRoles(userId, roleUpdateRequest, username);
        AuthzenResponse<UpdateUserResponse> response = new AuthzenResponse<>(updated);
        response.setMessage("User roles updated successfully");
        return ResponseEntity.ok(response);
    }

    /**
     * Creates a new system role. Can only be performed by admins.
     *
     * @param roleRequest Role creation details
     * @param request     Authenticated request
     * @return Confirmation message
     */
    @PostMapping(ApiEndpoint.ADMIN_ROLES)
    @Secured("ROLE_ADMIN")
    @PreAuthorize("hasAuthority('CREATE_USER')")
    public ResponseEntity<AuthzenResponse<String>> createRole(@RequestBody RoleRequest roleRequest,
                                             HttpServletRequest request) {
        String username = verifyAdmin(request);
        String created = adminEndpoint.createRole(roleRequest, username);
        AuthzenResponse<String> response = new AuthzenResponse<>();
        response.setMessage(created);
        return ResponseEntity.ok(response);
    }

    /**
     * Fetches the audit logs of critical admin operations like role updates or delegation.
     *
     * @param request HttpServletRequest containing the JWT token
     * @return List of audit log entries
     */
    @GetMapping(ApiEndpoint.ADMIN_AUDIT_LOGS)
    @Secured("ROLE_ADMIN")
    @PreAuthorize("hasAuthority('VIEW_USER')")
    public ResponseEntity<AuthzenResponse<List<AuditLogResponse>>> getAuditLogs(HttpServletRequest request) {
        String username = verifyAdmin(request);
        List<AuditLogResponse> auditLogs = adminEndpoint.getAuditLogs(username);
        AuthzenResponse<List<AuditLogResponse>> response = new AuthzenResponse<>(auditLogs);
        response.setMessage("AuditLogs listed successfully.");
        return ResponseEntity.ok(response);
    }


    /**
     * Delegates certain admin permissions to another user.
     * Must be executed by an authenticated and authorized admin.
     *
     * @param delegateRequest Request containing target user and permissions
     * @param request         HttpServletRequest with admin credentials
     * @return Delegation status message
     */
    @PostMapping(ApiEndpoint.ADMIN_DELEGATE)
    @Secured("ROLE_ADMIN")
    @PreAuthorize("hasAuthority('UPDATE_USER')")
    public ResponseEntity<AuthzenResponse<String>> delegatePermissions(@RequestBody DelegateRequest delegateRequest,
                                                      HttpServletRequest request) {
        String username = verifyAdmin(request);
        String delegated = adminEndpoint.delegatePermissions(delegateRequest, username);
        AuthzenResponse<String> response = new AuthzenResponse<>();
        response.setMessage(delegated);
        return ResponseEntity.ok(response);
    }


}