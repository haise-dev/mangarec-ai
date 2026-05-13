package com.mangarec.domain.user.repository;

import com.mangarec.domain.user.entity.UserAuthIdentityEntity;
import com.mangarec.domain.user.entity.UserEntity;
import com.mangarec.domain.user.model.AuthProvider;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface UserAuthIdentityRepository extends JpaRepository<UserAuthIdentityEntity, UUID> {
    Optional<UserAuthIdentityEntity> findByProviderAndProviderSubject(AuthProvider provider, String providerSubject);

    Optional<UserAuthIdentityEntity> findByUserAndProvider(UserEntity user, AuthProvider provider);

    boolean existsByProviderAndProviderSubject(AuthProvider provider, String providerSubject);
}
