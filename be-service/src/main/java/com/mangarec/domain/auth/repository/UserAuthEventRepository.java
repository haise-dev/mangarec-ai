package com.mangarec.domain.auth.repository;

import com.mangarec.domain.auth.entity.UserAuthEventEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface UserAuthEventRepository extends JpaRepository<UserAuthEventEntity, UUID> {
}
