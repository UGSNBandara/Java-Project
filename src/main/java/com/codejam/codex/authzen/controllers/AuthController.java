package com.codejam.codex.authzen.controllers;

import com.codejam.codex.authzen.constants.ApiEndpoint;
import com.codejam.codex.authzen.dtos.inputs.*;
import com.codejam.codex.authzen.dtos.outputs.TokenResponse;
import com.codejam.codex.authzen.dtos.outputs.UserResponse;
import com.codejam.codex.authzen.responses.AuthzenResponse;
import com.codejam.codex.authzen.endpoint.AuthEndpoint;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Controller responsible for handling HTTP requests related to authentication,
 * including user registration, login, password reset, and OAuth login.
 */
@RestController
@RequestMapping(ApiEndpoint.AUTH)
public class AuthController {

    private final AuthEndpoint authEndpoint;

    @Autowired
    public AuthController(AuthEndpoint authEndpoint) {
        this.authEndpoint = authEndpoint;
    }

    /**
     * Registers a new user.
     *
     * @param request The registration request containing user details.
     * @return A ResponseEntity with the result of the registration.
     */
    @PostMapping(ApiEndpoint.AUTH_REGISTER)
    public ResponseEntity<AuthzenResponse<UserResponse>> register(@RequestBody RegisterRequest request) {
        try {
            UserResponse userResponse = authEndpoint.registerUser(request);
            AuthzenResponse<UserResponse> response = new AuthzenResponse<>(userResponse);
            response.setMessage("User registered successfully");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            throw new RuntimeException("An error occurred during registration");
        }
    }

    /**
     * Authenticates a user and issues a token.
     *
     * @param request The login request containing user credentials.
     * @return A ResponseEntity with the result of the login process.
     */
    @PostMapping(ApiEndpoint.AUTH_LOGIN)
    public ResponseEntity<AuthzenResponse<TokenResponse>> login(@RequestBody LoginRequest request) {
        try {
            TokenResponse token = authEndpoint.authenticateUser(request);
            if (token != null) {
                AuthzenResponse<TokenResponse> response = new AuthzenResponse<>(token);
                response.setMessage("User logged successfully");
                return ResponseEntity.ok(response);
            } else {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(new AuthzenResponse<>(null, false, "Invalid credentials"));
            }
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new AuthzenResponse<>(null, false, "An error occurred during login"));
        }
    }

    /**
     * Handles OAuth login.
     *
     * @param request The OAuth login request containing OAuth credentials.
     * @return A ResponseEntity with the result of the OAuth login.
     */
    @PostMapping(ApiEndpoint.AUTH_OAUTH)
    public ResponseEntity<AuthzenResponse<TokenResponse>> oauthLogin(@RequestBody OAuthRequest request) {
        try {
            TokenResponse oauthToken = authEndpoint.authenticateOAuth(request);
            if (oauthToken != null) {
                AuthzenResponse<TokenResponse> response = new AuthzenResponse<>(oauthToken);
                response.setMessage("User logged successfully");
                return ResponseEntity.ok(response);
            } else {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(new AuthzenResponse<>(null, false, "OAuth login failed"));
            }
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new AuthzenResponse<>(null, false, "An error occurred during OAuth login"));
        }
    }

    /**
     * Requests a password reset by sending an email or token.
     *
     * @param request The reset request containing the user's email.
     * @return A ResponseEntity with the result of the reset request.
     */
    @PostMapping(ApiEndpoint.AUTH_RESET_REQUEST)
    public ResponseEntity<AuthzenResponse<Object>> resetPasswordRequest(@RequestBody ResetRequest request) {
        try {
            boolean emailSent = authEndpoint.sendPasswordResetEmail(request);
            if (emailSent) {
                AuthzenResponse<Object> response = new AuthzenResponse<>();
                response.setMessage("Password reset email sent");
                return ResponseEntity.ok(response);
            } else {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(new AuthzenResponse<>(null, false, "Failed to send reset email"));
            }
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new AuthzenResponse<>(null, false, "An error occurred while sending reset request"));
        }
    }

    /**
     * Resets the user's password using the provided token.
     *
     * @param request The reset password request containing token and new password.
     * @return A ResponseEntity with the result of the password reset.
     */
    @PostMapping(ApiEndpoint.AUTH_RESET_PASSWORD)
    public ResponseEntity<AuthzenResponse<Object>> resetPassword(@RequestBody ResetPasswordRequest request) {
        try {
            boolean isPasswordReset = authEndpoint.resetUserPassword(request);
            if (isPasswordReset) {
                AuthzenResponse<Object> response = new AuthzenResponse<>();
                response.setMessage("Password reset successfully");
                return ResponseEntity.ok(response);
            } else {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(new AuthzenResponse<>(null, false, "Failed to reset password"));
            }
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new AuthzenResponse<>(null, false, "An error occurred during password reset"));
        }
    }

    /**
     * Refreshes the access token using a valid refresh token.
     *
     * @param request RefreshTokenRequest with refresh token
     * @return New access and refresh token pair
     */
    @PostMapping(ApiEndpoint.AUTH_REFRESH)
    public ResponseEntity<AuthzenResponse<TokenResponse>> refreshToken(@RequestBody RefreshTokenRequest request) {
        try {
            TokenResponse tokenResponse = authEndpoint.refreshToken(request.getRefreshToken());
            AuthzenResponse<TokenResponse> response = new AuthzenResponse<>(tokenResponse);
            response.setMessage("User refreshed successfully");
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new AuthzenResponse<>(null, false, e.getMessage()));
        }
    }
}