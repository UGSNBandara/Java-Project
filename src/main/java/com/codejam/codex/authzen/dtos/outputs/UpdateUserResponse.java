package com.codejam.codex.authzen.dtos.outputs;

import com.codejam.codex.authzen.models.User;
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
public class UpdateUserResponse {
    private Long id;
    private String username;
    private String email;
    private Set<String> roles;

    public static UpdateUserResponse fromEntity(User user) {
        Set<String> roleNames = user.getUserRoles().stream()
                .map(userRole -> userRole.getRole().getName())
                .collect(Collectors.toSet());



        return UpdateUserResponse.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .roles(roleNames)
                .build();
    }
}
