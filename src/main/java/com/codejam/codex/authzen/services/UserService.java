package com.codejam.codex.authzen.services;

import com.codejam.codex.authzen.dtos.inputs.UpdateUserRequest;
import com.codejam.codex.authzen.dtos.outputs.UpdateUserResponse;
import com.codejam.codex.authzen.dtos.outputs.UserResponse;
import com.codejam.codex.authzen.models.User;
import com.codejam.codex.authzen.repositories.UserRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.*;
import org.springframework.security.core.userdetails.User.UserBuilder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuditEventRepository auditEventRepository;

    public UserResponse loadUserByUsername(String usernameOrEmail) throws UsernameNotFoundException {
        Optional<User> userOptional = userRepository.findByUsernameOrEmail(usernameOrEmail, usernameOrEmail);
        if (userOptional.isEmpty()) {
            throw new UsernameNotFoundException("User not found with username or email: " + usernameOrEmail);
        }
        
        User user = userOptional.get();
        Set<String> roles = user.getUserRoles()
                .stream()
                .map(userRole -> userRole.getRole().getName())
                .collect(Collectors.toSet());

        return UserResponse.builder()
                .email(user.getEmail())
                .username(user.getUsername())
                .roles(roles)
                .build();
    }


    public UserResponse getProfile(String username) {
        Optional<User> userOptional = userRepository.findByUsername(username);
        if (userOptional.isEmpty()) {
            throw new UsernameNotFoundException("User not found with username: " + username);
        }
        
        User user = userOptional.get();
        List<String> permissionNames = userRepository.findPermissionNamesByUsername(username);
        
        return UserResponse.fromEntity(user, permissionNames);
    }

    @Transactional
    public UpdateUserResponse updateUser(String username, UpdateUserRequest updateRequest) {
        Optional<User> userOptional = userRepository.findByUsername(username);
        if (userOptional.isEmpty()) {
            throw new UsernameNotFoundException("User not found with username: " + username);
        }
        
        User user = userOptional.get();
        
        // Log user profile update attempt
        auditEventRepository.save(AuditEvent.builder()
                .eventType("USER_PROFILE_UPDATE")
                .userId(user.getId())
                .username(user.getUsername())
                .ipAddress(updateRequest.getIp())
                .userAgent(updateRequest.getUserAgent())
                .description("User profile update attempt")
                .success(true)
                .authenticationMethod("LOCAL")
                .build());
        
        // Update username if provided and different
        if (updateRequest.getUsername() != null && !updateRequest.getUsername().isBlank() && !updateRequest.getUsername().equals(username)) {
            user.setUsername(updateRequest.getUsername());
        }

        // Update email if provided and different
        if (updateRequest.getEmail() != null && !updateRequest.getEmail().isBlank() && !updateRequest.getEmail().equals(user.getEmail())) {
            user.setEmail(updateRequest.getEmail());
        }

        // Update password if provided
        if (updateRequest.getPassword() != null && !updateRequest.getPassword().isBlank()) {
            user.setPassword(passwordEncoder.encode(updateRequest.getPassword()));
        }

        user = userRepository.save(user);
        
        // Log successful user profile update
        auditEventRepository.save(AuditEvent.builder()
                .eventType("USER_PROFILE_UPDATED")
                .userId(user.getId())
                .username(user.getUsername())
                .ipAddress(updateRequest.getIp())
                .userAgent(updateRequest.getUserAgent())
                .description("User profile updated successfully")
                .success(true)
                .authenticationMethod("LOCAL")
                .build());
        
        return UpdateUserResponse.fromEntity(user);
    }

}
