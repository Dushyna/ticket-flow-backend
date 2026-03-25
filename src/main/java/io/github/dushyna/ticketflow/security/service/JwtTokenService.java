package io.github.dushyna.ticketflow.security.service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.time.Instant;
import java.util.Date;
import java.util.Objects;
import java.util.function.Function;

/**
 * Service for generation, validation and parsing of JWT tokens.
 */
@Service
public class JwtTokenService {

    public enum TokenType {
        ACCESS,
        REFRESH
    }

    @Value("${jwt.at.live-in-min}")
    private int accessTokenLiveInMinutes;
    @Value("${jwt.rt.live-in-min}")
    private int refreshTokenLiveInMinutes;

    private final SecretKey accessTokenKey;
    private final SecretKey refreshTokenKey;

    public JwtTokenService(
            @Value("${jwt.at.secret}") String accessPhrase,
            @Value("${jwt.rt.secret}") String refreshPhrase
    ) {

        Objects.requireNonNull(accessPhrase, "Access token secret is null");
        Objects.requireNonNull(refreshPhrase, "Refresh token secret is null");
        if (accessPhrase.length() < 32 || refreshPhrase.length() < 32) {
            throw new IllegalArgumentException("Secrets must be at least 32 characters long");
        }
        this.accessTokenKey = Keys.hmacShaKeyFor(Decoders.BASE64.decode(accessPhrase));
        this.refreshTokenKey = Keys.hmacShaKeyFor(Decoders.BASE64.decode(refreshPhrase));
    }

    /**
     * Extracts subject (username) from given JWT token by type.
     */
    public String getUsernameFromToken(String token, TokenType tokenType) {
        SecretKey key = selectKey(tokenType);
        return extractClaim(token, Claims::getSubject, key);
    }

    /**
     * Generic extractor for any claim from token.
     */
    public <T> T extractClaim(String token,
                              Function<Claims, T> claimsResolver,
                              SecretKey key) {
        Claims claims = parseClaims(token, key);
        return claimsResolver.apply(claims);
    }

    /**
     * Generate access token with username and roles.
     */
    public String generateAccessToken(String userEmail) {
        Instant now = Instant.now();
        Instant expiry = now.plusSeconds(accessTokenLiveInMinutes * 60L);
        return Jwts.builder()
                .subject(userEmail)
                .expiration(Date.from(expiry))
                .signWith(accessTokenKey, Jwts.SIG.HS256)
                .compact();
    }

    /**
     * Generate refresh token with username only.
     */
    public String generateRefreshToken(String userEmail) {
        Instant now = Instant.now();
        Instant expiry = now.plusSeconds(refreshTokenLiveInMinutes * 60L);
        return Jwts.builder()
                .subject(userEmail)
                .expiration(Date.from(expiry))
                .signWith(refreshTokenKey, Jwts.SIG.HS256)
                .compact();
    }

    /**
     * Validate given token type.
     */
    public boolean validateToken(String token, TokenType tokenType) {
        try {
            parseClaims(token, selectKey(tokenType));
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Internal: parse and verify token signature and expiration using old API.
     */
    private Claims parseClaims(String token, SecretKey key) {
        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    private SecretKey selectKey(TokenType tokenType) {
        return tokenType == TokenType.ACCESS ? accessTokenKey : refreshTokenKey;
    }
}
