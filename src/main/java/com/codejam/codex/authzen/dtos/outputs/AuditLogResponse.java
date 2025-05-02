package com.codejam.codex.authzen.dtos.outputs;

import com.codejam.codex.authzen.models.User;
import jakarta.persistence.Column;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import lombok.*;

import java.sql.Timestamp;

@Getter
@Setter
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class AuditLogResponse {
    private Long id;
    private String username;
    private String actionType;
    private String ipAddress;
    private Timestamp timestamp;
}
