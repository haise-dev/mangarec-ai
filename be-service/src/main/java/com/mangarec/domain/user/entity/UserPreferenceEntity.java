package com.mangarec.domain.user.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.MapsId;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@Entity
@Table(name = "user_preferences", schema = "mangarec")
public class UserPreferenceEntity {
    @Id
    @Column(name = "user_id")
    private UUID userId;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @MapsId
    @JoinColumn(name = "user_id")
    private UserEntity user;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "preferred_genres_json", nullable = false, columnDefinition = "jsonb")
    private String preferredGenresJson = "[]";

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "excluded_tags_json", nullable = false, columnDefinition = "jsonb")
    private String excludedTagsJson = "[]";

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "preferred_languages_json", nullable = false, columnDefinition = "jsonb")
    private String preferredLanguagesJson = "[]";

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}
