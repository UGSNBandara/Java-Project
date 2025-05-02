package com.codejam.codex.authzen.repositories;

import com.codejam.codex.authzen.models.User;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);

    Optional<User> findByUsername(String username);

    Optional<User> findByUsernameOrEmail(String username, String email);

    @Query("SELECT DISTINCT p.name FROM User u " +
            "JOIN u.userRoles ur " +
            "JOIN ur.role r " +
            "JOIN r.rolePermissions rp " +
            "JOIN rp.permission p " +
            "WHERE u.username = :username")
    List<String> findPermissionNamesByUsername(@Param("username") String username);
}
