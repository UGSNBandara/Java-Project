package com.codejam.codex.authzen.dtos.inputs;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class DelegateRequest {
    @NotNull
    private Long userId;

    @NotNull
    private String role;

    private String reason;
}
