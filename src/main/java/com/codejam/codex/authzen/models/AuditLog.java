package com.codejam.codex.authzen.models;

import jakarta.persistence.*;
import lombok.*;

import java.sql.Timestamp;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@ToString
@Table(name = "audit_logs")
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "actor_id")
    @ToString.Exclude
    private User user;

    @Column(name = "action_type", nullable = false)
    private String actionType;

    @Column(name = "ip_address")
    private String ipAddress;

    @Column(nullable = false)
    private Timestamp timestamp;
}
