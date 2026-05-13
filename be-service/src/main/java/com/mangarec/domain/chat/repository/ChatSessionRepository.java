package com.mangarec.domain.chat.repository;

import com.mangarec.domain.chat.entity.ChatSessionEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.UUID;

public interface ChatSessionRepository extends JpaRepository<ChatSessionEntity, UUID> {
    @Modifying
    @Query(value = """
            UPDATE mangarec.chat_sessions
            SET user_id = :userId,
                updated_at = CURRENT_TIMESTAMP
            WHERE guest_profile_id = :guestProfileId
              AND user_id IS NULL
            """, nativeQuery = true)
    int claimGuestSessions(@Param("guestProfileId") UUID guestProfileId, @Param("userId") UUID userId);
}
