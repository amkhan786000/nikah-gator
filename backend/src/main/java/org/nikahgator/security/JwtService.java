package org.nikahgator.security;

import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.Map;

import javax.crypto.SecretKey;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

@Service
public class JwtService {

    @Value("${app.jwt.secret}")
    private String secret;

    @Value("${app.jwt.expiration-ms}")
    private long expirationMs;

    private SecretKey key() {
        return Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    /** The token's subject is the user's id (app_users.id). */
    public String generateToken(Long id, Map<String, Object> claims) {
        Date now = new Date();
        return Jwts.builder().claims(claims).subject(String.valueOf(id)).issuedAt(now)
                .expiration(new Date(now.getTime() + expirationMs)).signWith(key()).compact();
    }

    /** The user id in a valid, unexpired token; throws for anything else. */
    public Long userIdFrom(String token) {
        Claims claims = Jwts.parser().verifyWith(key()).build().parseSignedClaims(token).getPayload();
        return Long.valueOf(claims.getSubject());
    }
}
