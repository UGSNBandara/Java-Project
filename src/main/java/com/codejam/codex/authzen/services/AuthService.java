package com.codejam.codex.authzen.services;

import com.codejam.codex.authzen.dtos.inputs.*;
import com.codejam.codex.authzen.dtos.outputs.TokenResponse;
import com.codejam.codex.authzen.dtos.outputs.UserResponse;
import com.codejam.codex.authzen.models.*;
import com.codejam.codex.authzen.repositories.*;
import com.codejam.codex.authzen.utils.EmailUtil;
import com.codejam.codex.authzen.utils.JwtService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.sql.Timestamp;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;


@Service
public class AuthService {

    private final JwtService jwtService;
    private final UserService userService;
    private final UserRepository userRepository;
    private final BCryptPasswordEncoder passwordEncoder;
    private final EmailUtil emailUtil;
    private final EmailTokenRepository emailTokenRepository;
    private final OauthProviderRepository oauthProviderRepository;
    private final OAuthService oAuthService;
    private final RoleRepository roleRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final AuditEventRepository auditEventRepository;
    private final Set<String> blacklistedTokens = new HashSet<>();

    @Autowired
    public AuthService(JwtService jwtService, UserService userService, UserRepository userRepository,
                       BCryptPasswordEncoder passwordEncoder, EmailUtil emailUtil, EmailTokenRepository emailTokenRepository, OauthProviderRepository oauthProviderRepository, OAuthService oAuthService, RoleRepository roleRepository, RefreshTokenRepository refreshTokenRepository, AuditEventRepository auditEventRepository) {
        this.jwtService = jwtService;
        this.userService = userService;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.emailUtil = emailUtil;
        this.emailTokenRepository = emailTokenRepository;
        this.oauthProviderRepository = oauthProviderRepository;
        this.oAuthService = oAuthService;
        this.roleRepository = roleRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.auditEventRepository = auditEventRepository;
    }

    /**
     * Registers a new user.
     *
     * @param request The registration request containing user details.
     * @return true if registration was successful, false otherwise.
     */
    public UserResponse registerUser(RegisterRequest request) {
        // Validate input
        if (request.getUsername() == null || request.getUsername().trim().isEmpty()) {
            throw new IllegalArgumentException("Username is required");
        }
        if (request.getEmail() == null || !request.getEmail().matches("[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}")) {
            throw new IllegalArgumentException("Invalid email format");
        }
        if (request.getPassword() == null || request.getPassword().length() < 8) {
            throw new IllegalArgumentException("Password must be at least 8 characters long");
        }

        // Check if user already exists
        if (userRepository.findByEmail(request.getEmail()).isPresent()) {
            throw new IllegalArgumentException("Email already registered");
        }
        if (userRepository.findByUsername(request.getUsername()).isPresent()) {
            throw new IllegalArgumentException("Username already taken");
        }

        List<Role> roles = roleRepository.findByName("ROLE_USER");
        if (roles.isEmpty()) {
            throw new RuntimeException("Default role not found: ROLE_USER");
        }
        Role userRole = roles.get(0);

        User user = User.builder()
                .username(request.getUsername())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .isActive(true)
                .isLocked(false)
                .createdAt(new java.sql.Timestamp(System.currentTimeMillis()))
                .userRoles(new HashSet<>())
                .build();

        UserRole userRoleMapping = UserRole.builder()
                .user(user)
                .role(userRole)
                .build();
        user.getUserRoles().add(userRoleMapping);

        user = userRepository.save(user);
        List<String> permissionNames = userRepository.findPermissionNamesByUsername(user.getUsername());

        // Log user registration
        auditEventRepository.save(AuditEvent.builder()
                .eventType("USER_REGISTRATION")
                .userId(user.getId())
                .username(user.getUsername())
                .ipAddress(request.getIp())
                .userAgent(request.getUserAgent())
                .description("User registered successfully")
                .success(true)
                .authenticationMethod("LOCAL")
                .build());

        return UserResponse.fromEntity(user, permissionNames);
    }


    /**
     * Authenticates a user and issues an access token.
     *
     * @param request The login request containing user credentials.
     * @return Access token if authentication is successful, null otherwise.
     */
    public TokenResponse authenticateUser(LoginRequest request) {
        // Check if token is blacklisted
        String token = extractTokenFromHeader(request);
        if (token != null && isBlacklisted(token)) {
            throw new SecurityException("Token has been blacklisted");
        }

        Optional<User> userOptional = userRepository.findByEmail(request.getEmail());
        if (userOptional.isPresent()) {
            User user = userOptional.get();
            
            // Check if account is active and not locked
            if (!user.isActive()) {
                throw new SecurityException("Account is deactivated");
            }
            if (user.isLocked()) {
                throw new SecurityException("Account is locked");
            }

            // Verify password using password encoder
            if (passwordEncoder.matches(request.getPassword(), user.getPassword())) {
                // Load user details and permissions first
                UserResponse userResponse = userService.loadUserByUsername(user.getUsername());
                List<String> permissionNames = userRepository.findPermissionNamesByUsername(user.getUsername());
                userResponse.setPermissions(permissionNames);

                // Generate tokens
                String accessToken = jwtService.generateAccessToken(userResponse);
                String refreshToken = jwtService.generateRefreshToken(userResponse);
                saveRefreshToken(user, refreshToken);

                // Log successful login
                auditEventRepository.save(AuditEvent.builder()
                        .eventType("LOGIN_SUCCESS")
                        .userId(user.getId())
                        .username(user.getUsername())
                        .ipAddress(request.getIp())
                        .userAgent(request.getUserAgent())
                        .description("User logged in successfully")
                        .success(true)
                        .authenticationMethod("LOCAL")
                        .build());

                return new TokenResponse(accessToken, refreshToken);
            }
        }

        // Log failed login attempt
        auditEventRepository.save(AuditEvent.builder()
                .eventType("LOGIN_ATTEMPT")
                .userId(request.getEmail())
                .username(request.getEmail())
                .ipAddress(request.getIp())
                .userAgent(request.getUserAgent())
                .description("Invalid credentials")
                .success(false)
                .authenticationMethod("LOCAL")
                .build());

        throw new SecurityException("Invalid credentials");
    }

    /**
     * Handles OAuth login and generates OAuth token.
     *
     * @param request The OAuth login request containing OAuth credentials.
     * @return OAuth token if successful, null otherwise.
     */
    public TokenResponse authenticateOAuth(OAuthRequest request) {
        if ("github".equalsIgnoreCase(request.getProvider())) {
            // Check for blacklisted tokens
            String token = extractTokenFromHeader(request);
            if (token != null && isBlacklisted(token)) {
                throw new SecurityException("Token has been blacklisted");
            }

            try {
                String oAuthAccessToken = oAuthService.getGithubAccessToken(request.getOauthToken());
                Map<String, Object> githubUser = oAuthService.getGithubUser(oAuthAccessToken);

                // Validate required GitHub user information
                if (githubUser == null || 
                    githubUser.get("id") == null || 
                    githubUser.get("email") == null || 
                    githubUser.get("login") == null) {
                    throw new RuntimeException("Invalid GitHub user profile");
                }

                String githubId = githubUser.get("id").toString();
                String githubEmail = (String) githubUser.get("email");
                String githubLogin = (String) githubUser.get("login");

                Optional<OauthProvider> providerOpt = oauthProviderRepository.findByProviderAndExternalUserId("github", githubId);
                User user;
                if (providerOpt.isPresent()) {
                    user = providerOpt.get().getUser();
                } else {
                    Optional<User> existingUserOpt = userRepository.findByEmail(githubEmail);
                    if (existingUserOpt.isPresent()) {
                        user = existingUserOpt.get();
                    } else {
                        // Create new user with default role
                        List<Role> roles = roleRepository.findByName("ROLE_USER");
                        if (roles.isEmpty()) {
                            throw new RuntimeException("Default role not found: ROLE_USER");
                        }
                        Role userRole = roles.get(0);

                        user = User.builder()
                                .username(githubLogin)
                                .email(githubEmail)
                                .isActive(true)
                                .isLocked(false)
                                .userRoles(new HashSet<>())
                                .build();

                        UserRole userRoleMapping = UserRole.builder()
                                .user(user)
                                .role(userRole)
                                .build();
                        user.getUserRoles().add(userRoleMapping);

                        user = userRepository.save(user);
                    }
                    
                    // Save OAuth provider information
                    oauthProviderRepository.save(OauthProvider.builder()
                            .provider("github")
                            .externalUserId(githubId)
                            .user(user)
                            .build());
                }

                // Load user details using username
                UserResponse userResponse = userService.loadUserByUsername(user.getUsername());
                
                // Generate tokens
                String accessToken = jwtService.generateAccessToken(userResponse);
                String refreshToken = jwtService.generateRefreshToken(userResponse);
                saveRefreshToken(user, refreshToken);

                return new TokenResponse(accessToken, refreshToken);
            } catch (Exception e) {
                throw new RuntimeException("GitHub OAuth authentication failed: " + e.getMessage(), e);
            }
        }

        return null;
    }


    /**
     * Sends a password reset email to the user.
     *
     * @param request The reset request containing the user's email.
     * @return true if email was sent successfully, false otherwise.
     */
    public boolean sendPasswordResetEmail(ResetRequest request) {
        Optional<User> userOptional = userRepository.findByEmail(request.getEmail());
        if (userOptional.isPresent()) {
            User user = userOptional.get();

            EmailToken token = EmailToken.builder()
                    .user(user)
                    .token(UUID.randomUUID().toString())
                    .purpose("RESET_PASSWORD")
                    .expiresAt(Timestamp.from(Instant.now().plus(15, ChronoUnit.MINUTES)))
                    .build();

            emailTokenRepository.save(token);

            String resetLink = "http://localhost:8080/reset-password/reset-password.html?token=" + token.getToken();

            String subject = "Password Reset Request";
            String body = "You have requested to reset your password. Click the link below to reset your password:\n" + resetLink;

            return emailUtil.sendPasswordResetEmail(request.getEmail(), subject, body, resetLink);
        }
        return false;
    }


    /**
     * Resets the user's password using the provided token.
     *
     * @param request The reset password request containing token and new password.
     * @return true if the password was successfully reset, false otherwise.
     */
    public boolean resetUserPassword(ResetPasswordRequest request) {
        Optional<EmailToken> tokenOptional = emailTokenRepository.findByToken(request.getToken());
        if (tokenOptional.isPresent()) {
            EmailToken token = tokenOptional.get();

            if (token.getExpiresAt().before(Timestamp.from(Instant.now()))) {
                return false;
            }

            if (!"RESET_PASSWORD".equals(token.getPurpose())) {
                return false;
            }

            Optional<User> userOptional = userRepository.findByEmail(request.getEmail());
            if (userOptional.isPresent()) {
                User user = userOptional.get();

                if (!user.equals(token.getUser())) {
                    return false;
                }

                user.setPassword(passwordEncoder.encode(request.getNewPassword()));
                userRepository.save(user);

                emailTokenRepository.delete(token);

                return true;
            }
        }
        return false;
    }

    /**
     * Checks if the user is authenticated by validating the token from the request.
     *
     * @param request The HTTP request containing the token.
     * @return true if the user is authenticated, false otherwise.
     */
    public boolean isAuthenticated(HttpServletRequest request) {
        final String token = extractTokenFromHeader(request);
        if ((token == null || !jwtService.isTokenValid(token)) && isBlacklisted(token) ) {
            return false;
        }

        final String username = jwtService.extractUsername(token);
        UserResponse userDetails = userService.loadUserByUsername(username);
        return jwtService.isTokenValid(token, userDetails);
    }

    /**
     * Extracts the token from the HTTP request header.
     *
     * @param request The HTTP request.
     * @return The token if present, null otherwise.
     */
    private String extractTokenFromHeader(HttpServletRequest request) {
        final String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            return authHeader.substring(7);
        }
        return null;
    }

    /**
     * Retrieves the username from the token in the request.
     *
     * @param request The HTTP request containing the token.
     * @return The username extracted from the token, or null if the token is invalid.
     */
    public String getUsername(HttpServletRequest request) {
        final String token = extractTokenFromHeader(request);
        if ((token == null || !jwtService.isTokenValid(token)) && isBlacklisted(token) ) {
            return null;
        }
        return jwtService.extractUsername(token);
    }

    /**
     * Retrieves user details from the username.
     *
     * @param username The username.
     * @return UserResponse with the user's details, or null if the user doesn't exist.
     */
    public UserResponse getUserDetails(String username) {
        try {
            return userService.loadUserByUsername(username);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Saves a refresh token for a user.
     *
     * @param user The user associated with the refresh token.
     * @param token The refresh token to be saved.
     */
    private void saveRefreshToken(User user, String token) {
        // Clean up expired tokens for this user
        refreshTokenRepository.deleteByUserAndExpiresAtBefore(
            user, 
            new Timestamp(System.currentTimeMillis())
        );

        RefreshToken refreshToken = RefreshToken.builder()
                .user(user)
                .token(token)
                .revoked(false)
                .expiresAt(Timestamp.from(Instant.now().plus(7, ChronoUnit.DAYS)))
                .build();

        refreshTokenRepository.save(refreshToken);
    }

    /**
     * Refreshes an access token using a valid refresh token.
     *
     * @param refreshToken The refresh token to be used for refreshing the access token.
     * @return TokenResponse containing new access and refresh tokens.
     * @throws RuntimeException if the refresh token is expired or invalid.
     */
    public TokenResponse refreshToken(String refreshToken) {
        // First check if token is blacklisted
        if (isBlacklisted(refreshToken)) {
            throw new RuntimeException("Refresh token has been blacklisted");
        }

        // Check if token exists in database
        RefreshToken tokenRecord = refreshTokenRepository.findByToken(refreshToken)
                .orElseThrow(() -> new RuntimeException("Refresh token not found"));

        // Check if token is expired
        if (tokenRecord.getExpiresAt().before(new Timestamp(System.currentTimeMillis()))) {
            throw new RuntimeException("Refresh token has expired");
        }

        // Check if token is revoked
        if (tokenRecord.isRevoked()) {
            throw new RuntimeException("Refresh token has been revoked");
        }

        // Validate JWT token
        if (!jwtService.isTokenValid(refreshToken)) {
            throw new RuntimeException("Invalid refresh token");
        }

        // Get user details
        String username = jwtService.extractUsername(refreshToken);
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // Generate new tokens
        List<String> permissionNames = userRepository.findPermissionNamesByUsername(user.getUsername());
        UserResponse userDetails = UserResponse.fromEntity(user, permissionNames);

        String newAccessToken = jwtService.generateAccessToken(userDetails);
        String newRefreshToken = jwtService.generateRefreshToken(userDetails);

        // Revoke old refresh token
        tokenRecord.setRevoked(true);
        refreshTokenRepository.save(tokenRecord);

        // Save new refresh token
        saveRefreshToken(user, newRefreshToken);

        return TokenResponse.builder()
                .accessToken(newAccessToken)
                .refreshToken(newRefreshToken)
                .build();
    }

    /**
     * Blacklists the token associated with the incoming request if valid and not already blacklisted.
     *
     * @param request HttpServletRequest containing the token to be blacklisted.
     * @return true if the token was successfully added to the blacklist; false if the token is invalid
     *         or already blacklisted.
     */
    public boolean blacklistToken(HttpServletRequest request) {
        String token = extractTokenFromHeader(request);
        if (token == null || !jwtService.isTokenValid(token)) {
            return false;
        }

        if (isBlacklisted(token)) {
            return false;
        }

        blacklistedTokens.add(token);
        return true;
    }

    /**
     * Checks if a token is blacklisted.
     *
     * @param token The token to check.
     * @return true if the token is blacklisted, false otherwise.
     */
    public boolean isBlacklisted(String token) {
        return blacklistedTokens.contains(token);
    }


}
