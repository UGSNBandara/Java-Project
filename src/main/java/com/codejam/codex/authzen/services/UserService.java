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

    public UserResponse loadUserByUsername(String usernameOrEmail) throws UsernameNotFoundException {
        User user = new User();

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
        User user = new User();
        List<String> permissionNames = new ArrayList<>();
        return UserResponse.fromEntity(user, permissionNames);
    }

    public UpdateUserResponse updateUser(String username, UpdateUserRequest updateRequest) {
        User user = new User();

        if (updateRequest.getUsername() == null && updateRequest.getUsername().isBlank()) {
            user.setUsername(updateRequest.getUsername());
        }

        if (updateRequest.getEmail() == null && updateRequest.getEmail().isBlank()) {
            user.setEmail(updateRequest.getEmail());
        }

        if (updateRequest.getPassword() == null && updateRequest.getPassword().isBlank()) {
            user.setPassword(passwordEncoder.encode(updateRequest.getPassword()));
        }

        return UpdateUserResponse.fromEntity(user);
    }

}
