package com.codejam.codex.authzen.controllers;

import com.codejam.codex.authzen.constants.ApiEndpoint;
import com.codejam.codex.authzen.dtos.inputs.UpdateUserRequest;
import com.codejam.codex.authzen.dtos.outputs.UpdateUserResponse;
import com.codejam.codex.authzen.dtos.outputs.UserResponse;
import com.codejam.codex.authzen.endpoint.AuthEndpoint;
import com.codejam.codex.authzen.endpoint.UserEndpoint;
import com.codejam.codex.authzen.responses.AuthzenResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.annotation.Secured;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * Handles secured endpoints related to authenticated user actions such as
 * viewing/updating profile, refreshing tokens, and logout.
 */
@RestController
@RequestMapping(ApiEndpoint.USER)
@PreAuthorize("hasRole('USER')")
public class UserController {

    private final AuthEndpoint authEndpoint;
    private final UserEndpoint userEndpoint;

    @Autowired
    public UserController(AuthEndpoint authEndpoint, UserEndpoint userEndpoint) {
        this.authEndpoint = authEndpoint;
        this.userEndpoint = userEndpoint;
    }

    /**
     * Retrieves the authenticated user's profile.
     *
     * @param request HttpServletRequest with access token
     * @return User profile in standardized response format
     */
    @PreAuthorize("hasAuthority('VIEW_USER')")
    @GetMapping(ApiEndpoint.AUTH_ME)
    @Secured("ROLE_USER")
    public ResponseEntity<AuthzenResponse<UserResponse>> getProfile(HttpServletRequest request) {
        if (!authEndpoint.isAuthenticated(request)) {
            AuthzenResponse<UserResponse> response = new AuthzenResponse<>(null, false, "Unauthorized: Invalid or missing token");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
        }

        String username = authEndpoint.getUsername(request);
        if (username == null) {
            AuthzenResponse<UserResponse> response = new AuthzenResponse<>(null, false, "Unauthorized: Cannot extract username");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
        }

        UserResponse profile = userEndpoint.getProfile(username);
        AuthzenResponse<UserResponse> response = new AuthzenResponse<>(profile);
        response.setMessage("User profile retrieved successfully");
        return ResponseEntity.ok(response);
    }


    /**
     * Updates the authenticated user's profile.
     *
     * @param request       HttpServletRequest with access token
     * @param updateRequest Updated user information
     * @return Success message
     */
    @PreAuthorize("hasAuthority('UPDATE_USER')")
    @PutMapping(ApiEndpoint.AUTH_UPDATE)
    @Secured("ROLE_USER")
    public ResponseEntity<AuthzenResponse<UpdateUserResponse>> updateProfile(
            HttpServletRequest request,
            @RequestBody UpdateUserRequest updateRequest
    ) {
        if (!authEndpoint.isAuthenticated(request)) {
            AuthzenResponse<UpdateUserResponse> response = new AuthzenResponse<>(null, false, "Unauthorized");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
        }

        String username = authEndpoint.getUsername(request);
        UpdateUserResponse updateUserResponse = userEndpoint.updateUser(username, updateRequest);

        AuthzenResponse<UpdateUserResponse> response = new AuthzenResponse<>(updateUserResponse);
        response.setMessage("User updated successfully");
        return ResponseEntity.ok(response);
    }

    /**
     * Logs out the authenticated user.
     * Note: This is a stateless operation unless token blacklisting is implemented.
     *
     * @param request HttpServletRequest with access token
     * @return Success message
     */
    @PreAuthorize("hasAuthority('USER_LOGOUT')")
    @PostMapping(ApiEndpoint.AUTH_LOGOUT)
    @Secured("ROLE_USER")
    public ResponseEntity<AuthzenResponse<Object>> logout(HttpServletRequest request) {
        if (!authEndpoint.isAuthenticated(request)) {
            AuthzenResponse<Object> response = new AuthzenResponse<>(null, false, "Unauthorized");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
        }

        boolean blacklisted = authEndpoint.blacklistToken(request);

        if (blacklisted) {
            AuthzenResponse<Object> response = new AuthzenResponse<>();
            response.setMessage("User logged out successfully");
            return ResponseEntity.ok(response);
        } else {
            AuthzenResponse<Object> response = new AuthzenResponse<>(null, false, "Failed to blacklist token");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }



}