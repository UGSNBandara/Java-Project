package com.codejam.codex.authzen.repositories;

import com.codejam.codex.authzen.models.RolePermission;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RolePermissionRepository extends JpaRepository<RolePermission, Long> {

}
