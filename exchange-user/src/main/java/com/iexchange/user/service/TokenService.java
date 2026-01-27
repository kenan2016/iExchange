package com.iexchange.user.service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import javax.crypto.SecretKey;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * JWT Token 服务。
 */
@Service
public class TokenService {

    private static final long DEFAULT_EXPIRE_SECONDS = 7200;

    private final SecretKey secretKey;
    private final String issuer;
    private final Duration ttl;

    public TokenService(@Value("${jwt.secret:iexchange-demo-secret-please-change-32bytes}") String secret,
                        @Value("${jwt.issuer:iexchange}") String issuer,
                        @Value("${jwt.expire-seconds:7200}") long expireSeconds) {
        this.secretKey = buildSecretKey(secret);
        this.issuer = issuer == null ? "iexchange" : issuer;
        long seconds = expireSeconds > 0 ? expireSeconds : DEFAULT_EXPIRE_SECONDS;
        this.ttl = Duration.ofSeconds(seconds);
    }

    /**
     * 生成 JWT Token。
     *
     * @param userId 用户 ID
     * @param username 用户名
     * @return JWT 字符串
     */
    public String createToken(Long userId, String username) {
        Instant now = Instant.now();
        return Jwts.builder()
            .setIssuer(issuer)
            .setSubject(String.valueOf(userId))
            .claim("username", username)
            .setIssuedAt(Date.from(now))
            .setExpiration(Date.from(now.plus(ttl)))
            .signWith(secretKey, SignatureAlgorithm.HS256)
            .compact();
    }

    /**
     * 解析 JWT 并返回用户信息。
     *
     * @param token JWT 字符串
     * @return 用户信息，解析失败返回 null
     */
    public JwtUser parseToken(String token) {
        if (token == null || token.trim().isEmpty()) {
            return null;
        }
        try {
            Claims claims = Jwts.parserBuilder()
                .setSigningKey(secretKey)
                .build()
                .parseClaimsJws(token)
                .getBody();
            if (issuer != null && !issuer.isEmpty() && !issuer.equals(claims.getIssuer())) {
                return null;
            }
            String subject = claims.getSubject();
            if (subject == null || subject.trim().isEmpty()) {
                return null;
            }
            Long userId = Long.valueOf(subject);
            String username = claims.get("username", String.class);
            return new JwtUser(userId, username);
        } catch (JwtException | IllegalArgumentException ex) {
            return null;
        }
    }

    /**
     * 获取 Token 过期时间（秒）。
     */
    public long getExpireSeconds() {
        return ttl.getSeconds();
    }

    private SecretKey buildSecretKey(String secret) {
        String normalized = secret == null ? "" : secret.trim();
        if (normalized.length() < 32) {
            normalized = (normalized + "00000000000000000000000000000000").substring(0, 32);
        }
        return Keys.hmacShaKeyFor(normalized.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * JWT 解析后的用户信息。
     */
    public static class JwtUser {

        /**
         * 用户 ID。
         */
        private final Long userId;

        /**
         * 用户名。
         */
        private final String username;

        public JwtUser(Long userId, String username) {
            this.userId = userId;
            this.username = username;
        }

        public Long getUserId() {
            return userId;
        }

        public String getUsername() {
            return username;
        }
    }
}
