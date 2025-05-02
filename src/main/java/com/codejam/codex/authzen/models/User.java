package com.codejam.codex.authzen.models;


import jakarta.persistence.*;
import lombok.*;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@ToString
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, name = "username")
    private String username;

    @Column(nullable = false, name = "email")
    private String email;

    @Column(nullable = false, name = "password")
    private String password;

    @Column(nullable = false, name = "is_active")
    private boolean isActive;

    @Column(nullable = false, name = "is_locked")
    private boolean isLocked;

    @Column(nullable = false, name = "created_at")
    private Timestamp createdAt;

    @OneToMany(mappedBy = "user", cascade = {CascadeType.ALL}, orphanRemoval = true)
    @Builder.Default
    private List<RefreshToken> refreshTokens = new ArrayList<>();


    public void addRefreshToken(RefreshToken refreshToken) {
        refreshTokens.add(refreshToken);
        refreshToken.setUser(this);
    }

    public void removeRefreshToken(RefreshToken refreshToken) {
        refreshTokens.remove(refreshToken);
        refreshToken.setUser(null);
    }

    @OneToMany(mappedBy = "user", cascade = {CascadeType.ALL}, orphanRemoval = true)
    @Builder.Default
    private List<EmailToken> emailTokens = new ArrayList<>();


    public void addEmailTokens(EmailToken emailToken) {
        emailTokens.add(emailToken);
        emailToken.setUser(this);
    }

    public void removeEmailToken(EmailToken emailToken) {
        refreshTokens.remove(emailToken);
        emailToken.setUser(null);
    }

    @OneToMany(mappedBy = "user", cascade = {CascadeType.ALL}, orphanRemoval = true)
    @Builder.Default
    private List<OauthProvider> oauthProviders = new ArrayList<>();


    public void addOauthProvider(OauthProvider oauthProvider) {
        oauthProviders.add(oauthProvider);
        oauthProvider.setUser(this);
    }

    public void removeOauthProvider(OauthProvider oauthProvider) {
        oauthProviders.remove(oauthProvider);
        oauthProvider.setUser(null);
    }

    @OneToMany(mappedBy = "user", cascade = {CascadeType.ALL}, orphanRemoval = true)
    @Builder.Default
    private List<AuditLog> auditLogs = new ArrayList<>();


    public void addAuditLog(AuditLog auditLog) {
        auditLogs.add(auditLog);
        auditLog.setUser(this);
    }

    public void removeAuditLog(AuditLog auditLog) {
        oauthProviders.remove(auditLog);
        auditLog.setUser(null);
    }

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<UserRole> userRoles = new HashSet<>();
}
