package com.mangarec.security;

import com.mangarec.domain.user.entity.UserEntity;
import com.mangarec.exception.UnauthorizedException;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.security.Key;
import java.time.Instant;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Service
public class JwtServiceImpl implements JwtService {
    @Value("${jwt.expiryMinutes}")
    private long expiryMinutes;

    @Value("${jwt.expiryDay}")
    private long expiryDay;

    @Value("${jwt.accessKey}")
    private String accessKey;

    @Value("${jwt.refreshKey}")
    private String refreshKey;

    @Override
    public String generateAccessToken(UserEntity user) {
        Map<String, Object> claims = baseClaims(user, TokenType.ACCESS_TOKEN);
        Instant expiresAt = getAccessTokenExpiresAt();

        return buildToken(user.getId(), claims, expiresAt, getKey(TokenType.ACCESS_TOKEN));
    }

    @Override
    public String generateRefreshToken(UserEntity user) {
        Map<String, Object> claims = baseClaims(user, TokenType.REFRESH_TOKEN);
        Instant expiresAt = getRefreshTokenExpiresAt();

        return buildToken(user.getId(), claims, expiresAt, getKey(TokenType.REFRESH_TOKEN));
    }

    @Override
    public Claims extractClaims(String token, TokenType tokenType) {
        try {
            return Jwts.parserBuilder()
                    .setSigningKey(getKey(tokenType))
                    .build()
                    .parseClaimsJws(token)
                    .getBody();
        } catch (ExpiredJwtException e) {
            throw new UnauthorizedException("Token expired");
        } catch (JwtException | IllegalArgumentException e) {
            throw new UnauthorizedException("Invalid token");
        }
    }

    @Override
    public UUID extractUserId(String token, TokenType tokenType) {
        String subject = extractClaims(token, tokenType).getSubject();
        try {
            return UUID.fromString(subject);
        } catch (IllegalArgumentException e) {
            throw new UnauthorizedException("Invalid token subject");
        }
    }

    @Override
    public Instant getAccessTokenExpiresAt() {
        return Instant.now().plusSeconds(expiryMinutes * 60);
    }

    @Override
    public Instant getRefreshTokenExpiresAt() {
        return Instant.now().plusSeconds(expiryDay * 24 * 60 * 60);
    }

    private Map<String, Object> baseClaims(UserEntity user, TokenType tokenType) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("email", user.getEmail());
        claims.put("role", user.getRole().name());
        claims.put("token_type", tokenType.name());
        return claims;
    }

    private String buildToken(UUID userId, Map<String, Object> claims, Instant expiresAt, Key key) {
        Instant now = Instant.now();
        return Jwts.builder()
                .setClaims(claims)
                .setSubject(userId.toString())
                .setIssuedAt(Date.from(now))
                .setExpiration(Date.from(expiresAt))
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();
    }

    private Key getKey(TokenType tokenType) {
        String key = tokenType == TokenType.ACCESS_TOKEN ? accessKey : refreshKey;
        return Keys.hmacShaKeyFor(Decoders.BASE64.decode(key));
    }
}
