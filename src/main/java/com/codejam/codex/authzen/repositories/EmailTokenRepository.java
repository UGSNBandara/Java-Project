package com.codejam.codex.authzen.repositories;

import com.codejam.codex.authzen.models.EmailToken;
import jakarta.validation.constraints.NotBlank;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface EmailTokenRepository extends JpaRepository<EmailToken, Long> {
    Optional<EmailToken> findByToken(@NotBlank String token);
}
