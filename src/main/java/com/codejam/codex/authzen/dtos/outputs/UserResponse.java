package com.codejam.codex.authzen.dtos.outputs;

import com.codejam.codex.authzen.models.User;
import com.codejam.codex.authzen.repositories.UserRepository;
import lombok.*;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Getter
@Setter
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class UserResponse {
    private Long id;
    private String username;
    private String email;
    private Set<String> roles;
    private List<String> permissions;

    public static UserResponse fromEntity(User user, List<String> permissionNames) {
        Set<String> roleNames = user.getUserRoles().stream()
                .map(userRole -> userRole.getRole().getName())
                .collect(Collectors.toSet());



        return UserResponse.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .roles(roleNames)
                .permissions(permissionNames)
                .build();
    }

}
