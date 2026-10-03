package com.ticketbooking.concert_booking_platform.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.UUID;

@Component
public class QueueTokenService {

    @Value("${app.jwt.secret}") // tái dùng cùng secret với auth JWT — đơn giản cho scope này
    private String secret;

    private SecretKey key() {
        return Keys.hmacShaKeyFor(secret.getBytes());
    }

    public String generateQueueToken(Long userId, Long concertId, long ttlMinutes) {
        String queueEntryId = UUID.randomUUID().toString();
        Date now = new Date();
        Date expiry = new Date(now.getTime() + ttlMinutes * 60_000);

        return Jwts.builder()
                .subject(String.valueOf(userId))
                .claim("concertId", concertId)
                .claim("queueEntryId", queueEntryId)
                .claim("type", "QUEUE")
                .issuedAt(now)
                .expiration(expiry)
                .signWith(key())
                .compact();
    }

    public String generateAdmissionToken(Long userId, Long concertId, long ttlMinutes) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + ttlMinutes * 60_000);

        return Jwts.builder()
                .subject(String.valueOf(userId))
                .claim("concertId", concertId)
                .claim("type", "ADMISSION")
                .issuedAt(now)
                .expiration(expiry)
                .signWith(key())
                .compact();
    }

    public Claims parseClaims(String token) {
        return Jwts.parser().verifyWith(key()).build().parseSignedClaims(token).getPayload();
    }

    public boolean isValidAdmissionToken(String token, Long expectedConcertId) {
        try {
            Claims claims = parseClaims(token);
            return "ADMISSION".equals(claims.get("type", String.class))
                    && expectedConcertId.equals(claims.get("concertId", Long.class));
        } catch (Exception e) {
            return false;
        }
    }
}