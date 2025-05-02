package com.codejam.codex.authzen.repositories;

import com.codejam.codex.authzen.models.UserRole;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRoleRepository extends JpaRepository<UserRole, Long> {

}
