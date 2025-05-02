package com.codejam.codex.authzen.dtos.inputs;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class OAuthRequest {
    @NotBlank
    private String provider;

    @NotBlank
    private String oauthToken;
}
