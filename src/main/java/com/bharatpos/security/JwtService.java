package com.bharatpos.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@Service
public class JwtService {

    @Value("${bharatpos.jwt.secret}")
    private String secret;

    @Value("${bharatpos.jwt.expiration}")
    private Long expiration;

    @Value("${bharatpos.jwt.refresh-expiration}")
    private Long refreshExpiration;

    private SecretKey getKey() {
        byte[] keyBytes = secret.getBytes();
        return Keys.hmacShaKeyFor(keyBytes);
    }

    public String generateAccessToken(Long userId, Long tenantId, Long storeId, String role) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("tenantId", tenantId);
        claims.put("storeId", storeId);
        claims.put("role", role);
        return buildToken(claims, userId.toString(), expiration);
    }

    public String generateRefreshToken(Long userId) {
        return buildToken(new HashMap<>(), userId.toString(), refreshExpiration);
    }

    private String buildToken(Map<String, Object> claims, String subject, Long expirationMs) {
        return Jwts.builder()
                .claims(claims)
                .subject(subject)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + expirationMs))
                .signWith(getKey())
                .compact();
    }

    public String extractUserId(String token) {
        return extractAllClaims(token).getSubject();
    }

    public Long extractTenantId(String token) {
        return extractAllClaims(token).get("tenantId", Long.class);
    }

    public Long extractStoreId(String token) {
        return extractAllClaims(token).get("storeId", Long.class);
    }

    public String extractRole(String token) {
        return extractAllClaims(token).get("role", String.class);
    }

    public boolean isTokenValid(String token) {
        try {
            extractAllClaims(token);
            return true;
        } catch (JwtException e) {
            return false;
        }
    }

    private Claims extractAllClaims(String token) {
        return Jwts.parser()
                .verifyWith(getKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}