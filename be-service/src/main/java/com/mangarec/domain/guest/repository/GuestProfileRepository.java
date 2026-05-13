package com.mangarec.domain.guest.repository;

import com.mangarec.domain.guest.entity.GuestProfileEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface GuestProfileRepository extends JpaRepository<GuestProfileEntity, UUID> {
    Optional<GuestProfileEntity> findByGuestId(String guestId);
}
