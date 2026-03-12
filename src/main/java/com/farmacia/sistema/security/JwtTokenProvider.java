package com.farmacia.sistema.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.stream.Collectors;

@Component
public class JwtTokenProvider {

    private final SecretKey key;
    private final long validityInMs;

    public JwtTokenProvider(@Value("${app.security.jwt.secret}") String secret,
                            @Value("${app.security.jwt.expiration-ms:3600000}") long validityInMs) {
        byte[] keyBytes = Decoders.BASE64.decode(secret);
        this.key = Keys.hmacShaKeyFor(keyBytes);
        this.validityInMs = validityInMs;
    }

    public String generateToken(Authentication authentication) {
        String username = authentication.getName();
        String roles = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.joining(","));

        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + validityInMs);

        var builder = Jwts.builder()
                .setSubject(username)
                .claim("roles", roles)
                .setIssuedAt(now)
                .setExpiration(expiryDate);

        if (authentication.getPrincipal() instanceof com.farmacia.sistema.security.TenantUserDetails tud
                && tud.getTenantId() != null) {
            builder.claim("tenantId", tud.getTenantId());
        }

        return builder.signWith(key, SignatureAlgorithm.HS256).compact();
    }

    public String getUsername(String token) {
        return parseClaims(token).getSubject();
    }

    public String getRoles(String token) {
        Object roles = parseClaims(token).get("roles");
        return roles != null ? roles.toString() : "";
    }

    /** Obtiene el tenantId del token (si existe). Para uso en API multi-tenant. */
    public Long getTenantId(String token) {
        Object tid = parseClaims(token).get("tenantId");
        if (tid instanceof Number) return ((Number) tid).longValue();
        if (tid != null) {
            try {
                return Long.parseLong(tid.toString());
            } catch (NumberFormatException ignored) { }
        }
        return null;
    }

    public boolean validateToken(String token) {
        try {
            parseClaims(token);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private Claims parseClaims(String token) {
        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}

