package com.codejam.codex.authzen.repositories;

import com.codejam.codex.authzen.models.Role;
import jakarta.validation.constraints.NotBlank;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface RoleRepository extends JpaRepository<Role, Long> {

    List<Role> findByName(String roleName);

    boolean existsByName(@NotBlank String roleName);

    @Query("SELECT r FROM Role r LEFT JOIN FETCH r.rolePermissions WHERE r.name = :name")
    Optional<Role> findByNameWithPermissions(@Param("name") String name);

}
