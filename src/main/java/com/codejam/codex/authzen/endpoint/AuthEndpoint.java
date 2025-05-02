package com.codejam.codex.authzen.endpoint;

import com.codejam.codex.authzen.dtos.inputs.*;
import com.codejam.codex.authzen.dtos.outputs.TokenResponse;
import com.codejam.codex.authzen.dtos.outputs.UserResponse;
import com.codejam.codex.authzen.services.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Endpoint responsible for handling authentication-related logic such as
 * extracting username, validating authentication, and refreshing tokens.
 */
@Component
public class AuthEndpoint {

    private final AuthService authService;

    @Autowired
    public AuthEndpoint(AuthService authService) {
        this.authService = authService;
    }

    /**
     * Checks if the incoming request contains a valid access token.
     *
     * @param request HttpServletRequest
     * @return true if authenticated; false otherwise
     */
    public boolean isAuthenticated(HttpServletRequest request) {
        return authService.isAuthenticated(request);
    }

    /**
     * Extracts the username from a valid JWT access token in the request.
     *
     * @param request HttpServletRequest
     * @return Username or null if token is invalid
     */
    public String getUsername(HttpServletRequest request) {
        return authService.getUsername(request);
    }

    /**
     * Validates and refreshes access and refresh tokens using the provided refresh token.
     *
     * @param refreshToken refresh token string
     * @return TokenResponse containing new tokens
     */
    public TokenResponse refreshToken(String refreshToken) {
        return authService.refreshToken(refreshToken);
    }

    /**
     * Registers a new user.
     *
     * @param request The registration request containing user details.
     * @return true if registration was successful, false otherwise.
     */
    public UserResponse registerUser(RegisterRequest request) {
        return authService.registerUser(request);
    }

    /**
     * Authenticates a user and returns an access token.
     *
     * @param request The login request containing user credentials.
     * @return TokenResponse with access token, or null if authentication fails.
     */
    public TokenResponse authenticateUser(LoginRequest request) {
        return authService.authenticateUser(request);
    }

    /**
     * Authenticates a user via OAuth and returns an access token.
     *
     * @param request The OAuth request containing credentials.
     * @return TokenResponse with OAuth token, or null if authentication fails.
     */
    public TokenResponse authenticateOAuth(OAuthRequest request) {
        return authService.authenticateOAuth(request);
    }

    /**
     * Sends a password reset email to the user.
     *
     * @param request The reset request containing the user's email.
     * @return true if email was successfully sent, false otherwise.
     */
    public boolean sendPasswordResetEmail(ResetRequest request) {
        return authService.sendPasswordResetEmail(request);
    }

    /**
     * Resets the user's password using a provided reset token.
     *
     * @param request The reset password request containing the token and new password.
     * @return true if the password was successfully reset, false otherwise.
     */
    public boolean resetUserPassword(ResetPasswordRequest request) {
        return authService.resetUserPassword(request);
    }


    /**
     * Blacklists the token associated with the incoming request.
     *
     * @param request HttpServletRequest containing the token to be blacklisted.
     * @return true if the token was successfully added to the blacklist; false otherwise.
     */
    public boolean blacklistToken(HttpServletRequest request) {
        return authService.blacklistToken(request);
    }

    /**
     * Fetches the details of a user by their username.
     * This method can be used to retrieve user-specific information, such as profile data, roles, and permissions.
     *
     * @param username String - The username of the user whose details are to be fetched.
     * @return UserResponse - Contains the user details, such as username, email, and other relevant information.
     */
    public UserResponse getUserDetails(String username) {
        return authService.getUserDetails(username);
    }}