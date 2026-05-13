package com.mangarec.domain.auth.repository;

import com.mangarec.domain.auth.entity.AuthActionTokenEntity;
import com.mangarec.domain.auth.model.AuthActionPurpose;
import com.mangarec.domain.user.entity.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public interface AuthActionTokenRepository extends JpaRepository<AuthActionTokenEntity, UUID> {
    Optional<AuthActionTokenEntity> findByPurposeAndTokenHashAndUsedAtIsNullAndExpiresAtAfter(
            AuthActionPurpose purpose,
            String tokenHash,
            Instant now
    );

    @Modifying
    @Query("""
            UPDATE AuthActionTokenEntity token
            SET token.usedAt = :usedAt
            WHERE token.user = :user
              AND token.purpose = :purpose
              AND token.usedAt IS NULL
              AND token.expiresAt > :now
            """)
    int markActiveTokensUsed(
            @Param("user") UserEntity user,
            @Param("purpose") AuthActionPurpose purpose,
            @Param("now") Instant now,
            @Param("usedAt") Instant usedAt
    );
}
