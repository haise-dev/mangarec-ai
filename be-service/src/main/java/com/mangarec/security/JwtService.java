package com.mangarec.security;

import com.mangarec.domain.user.entity.UserEntity;
import io.jsonwebtoken.Claims;

import java.time.Instant;
import java.util.UUID;

public interface JwtService {
    String generateAccessToken(UserEntity user);

    String generateRefreshToken(UserEntity user);

    Claims extractClaims(String token, TokenType tokenType);

    UUID extractUserId(String token, TokenType tokenType);

    Instant getAccessTokenExpiresAt();

    Instant getRefreshTokenExpiresAt();
}
