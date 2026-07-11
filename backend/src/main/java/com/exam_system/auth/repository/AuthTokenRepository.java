package com.exam_system.auth.repository;

import com.exam_system.auth.domain.AuthToken;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AuthTokenRepository extends JpaRepository<AuthToken, UUID> {
    Optional<AuthToken> findByAccessJti(String accessJti);

    Optional<AuthToken> findByRefreshJti(String refreshJti);

    List<AuthToken> findByUserIdAndRevokedAtIsNull(Long userId);
}
