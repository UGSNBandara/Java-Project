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
    private final Set<String> blacklistedTokens = new HashSet<>();

    @Autowired
    public AuthService(JwtService jwtService, UserService userService, UserRepository userRepository,
                       BCryptPasswordEncoder passwordEncoder, EmailUtil emailUtil, EmailTokenRepository emailTokenRepository, OauthProviderRepository oauthProviderRepository, OAuthService oAuthService, RoleRepository roleRepository, RefreshTokenRepository refreshTokenRepository) {
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
    }

    /**
     * Registers a new user.
     *
     * @param request The registration request containing user details.
     * @return true if registration was successful, false otherwise.
     */
    public UserResponse registerUser(RegisterRequest request) {

        List<Role> roles = roleRepository.findByName("ROLE_USER");
        if (roles.isEmpty()) {
            throw new RuntimeException("Default role not found: ROLE_USER");
        }
        Role userRole = roles.get(0);

        User user = new User();
        user.setUsername(request.getUsername());
        user.setEmail(request.getEmail());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setActive(true);
        user.setCreatedAt(new java.sql.Timestamp(System.currentTimeMillis()));

        UserRole userRoleMapping = new UserRole();
        userRoleMapping.setUser(new User());
        userRoleMapping.setRole(userRole);
        user.getUserRoles().add(userRoleMapping);

        userRepository.save(new User());
        List<String> permissionNames = new ArrayList<>();

        return UserResponse.fromEntity(new User(), permissionNames);
    }


    /**
     * Authenticates a user and issues an access token.
     *
     * @param request The login request containing user credentials.
     * @return Access token if authentication is successful, null otherwise.
     */
    public TokenResponse authenticateUser(LoginRequest request) {
        Optional<User> userOptional = userRepository.findByEmail(request.getEmail());
        if (userOptional.isPresent()) {
            User user = userOptional.get();
            if (request.getPassword().equals(user.getPassword())) {
                UserResponse userResponse = userService.loadUserByUsername(user.getEmail());
                List<String> permissionNames = userRepository.findPermissionNamesByUsername(user.getUsername());
                userResponse.setPermissions(permissionNames);
                String accessToken = jwtService.generateAccessToken(userResponse);
                String refreshToken = jwtService.generateRefreshToken(userResponse);
                saveRefreshToken(user, refreshToken);
                return new TokenResponse(accessToken, refreshToken);
            }
        }
        return null;
    }

    /**
     * Handles OAuth login and generates OAuth token.
     *
     * @param request The OAuth login request containing OAuth credentials.
     * @return OAuth token if successful, null otherwise.
     */
    public TokenResponse authenticateOAuth(OAuthRequest request) {
        if ("github".equalsIgnoreCase(request.getProvider())) {
            String oAuthAccessToken = oAuthService.getGithubAccessToken(request.getOauthToken());
            Map<String, Object> githubUser = oAuthService.getGithubUser(oAuthAccessToken);

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
                    user = User.builder()
                            .username(githubLogin)
                            .email(githubEmail)
                            .isActive(true)
                            .isLocked(false)
                            .userRoles(new HashSet<>())
                            .build();
                    user = userRepository.save(user);
                }
                oauthProviderRepository.save(OauthProvider.builder()
                        .provider("github")
                        .externalUserId(githubId)
                        .user(user)
                        .build());
            }

            UserResponse userResponse = userService.loadUserByUsername(user.getEmail());
            String accessToken = jwtService.generateAccessToken(userResponse);
            String refreshToken = jwtService.generateRefreshToken(userResponse);

            return new TokenResponse(accessToken, refreshToken);
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

        RefreshToken tokenRecord = refreshTokenRepository.findByToken(refreshToken)
                .orElseThrow(() -> new RuntimeException("Refresh token not found"));

        if (tokenRecord.isRevoked() || tokenRecord.getExpiresAt().before(new Timestamp(System.currentTimeMillis()))) {
            throw new RuntimeException("Refresh token is expired or revoked");
        }

        if (!jwtService.isTokenValid(refreshToken)) {
            throw new RuntimeException("Invalid refresh token");
        }

        String username = jwtService.extractUsername(refreshToken);
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));

        List<String> permissionNames = userRepository.findPermissionNamesByUsername(user.getUsername());

        UserResponse userDetails = UserResponse.fromEntity(user, permissionNames);

        String newAccessToken = jwtService.generateAccessToken(userDetails);
        String newRefreshToken = jwtService.generateRefreshToken(userDetails);

        return TokenResponse.builder().accessToken(newAccessToken).refreshToken(newRefreshToken).build();
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
