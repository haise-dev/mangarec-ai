package com.mangarec.security;

import com.mangarec.domain.user.entity.UserAuthIdentityEntity;
import com.mangarec.domain.user.entity.UserEntity;
import com.mangarec.domain.user.model.AuthProvider;
import com.mangarec.domain.user.repository.UserAuthIdentityRepository;
import com.mangarec.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Locale;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AppUserDetailsService implements UserDetailsService {
    private final UserRepository userRepository;
    private final UserAuthIdentityRepository authIdentityRepository;

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        String normalizedEmail = normalizeEmail(email);
        UserEntity user = userRepository.findByEmailIgnoreCase(normalizedEmail)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));
        UserAuthIdentityEntity localIdentity = authIdentityRepository
                .findByProviderAndProviderSubject(AuthProvider.LOCAL, normalizedEmail)
                .orElseThrow(() -> new UsernameNotFoundException("Local credential not found"));

        return new AuthenticatedUser(user, localIdentity);
    }

    public UserDetails loadUserById(UUID userId) {
        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));
        return new AuthenticatedUser(user);
    }

    private String normalizeEmail(String email) {
        return email == null ? "" : email.trim().toLowerCase(Locale.ROOT);
    }
}
