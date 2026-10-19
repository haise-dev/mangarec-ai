package com.mangarec.domain.user.repository;

import com.mangarec.domain.user.entity.UserRefreshTokenEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface UserRefreshTokenRepository extends JpaRepository<UserRefreshTokenEntity, UUID> {
}
