package com.mangarec.security;

import com.mangarec.domain.user.entity.UserAuthIdentityEntity;
import com.mangarec.domain.user.entity.UserEntity;
import com.mangarec.domain.user.model.UserStatus;
import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

@Getter
public class AuthenticatedUser implements UserDetails {
    private final UUID id;
    private final String email;
    private final String password;
    private final UserStatus status;
    private final List<GrantedAuthority> authorities;

    public AuthenticatedUser(UserEntity user, UserAuthIdentityEntity localIdentity) {
        this(user, localIdentity.getPasswordHash());
    }

    public AuthenticatedUser(UserEntity user) {
        this(user, (String) null);
    }

    private AuthenticatedUser(UserEntity user, String password) {
        this.id = user.getId();
        this.email = user.getEmail();
        this.password = password;
        this.status = user.getStatus();
        this.authorities = List.of(new SimpleGrantedAuthority("ROLE_" + user.getRole().name()));
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public String getUsername() {
        return email;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return status != UserStatus.DISABLED && status != UserStatus.DELETED;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return status == UserStatus.ACTIVE;
    }
}
