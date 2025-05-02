package com.codejam.codex.authzen.dtos.outputs;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@ToString
@Builder
public class TokenResponse {
    private String accessToken;
    private String refreshToken;
}

