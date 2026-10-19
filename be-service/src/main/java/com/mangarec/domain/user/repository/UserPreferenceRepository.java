package com.mangarec.domain.user.repository;

import com.mangarec.domain.user.entity.UserPreferenceEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface UserPreferenceRepository extends JpaRepository<UserPreferenceEntity, UUID> {
}
