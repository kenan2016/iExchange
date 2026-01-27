package com.iexchange.gateway.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import javax.crypto.SecretKey;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * JWT 校验服务。
 */
@Service
public class JwtTokenService {

    private final SecretKey secretKey;
    private final String issuer;

    public JwtTokenService(@Value("${jwt.secret:iexchange-demo-secret-please-change-32bytes}") String secret,
                           @Value("${jwt.issuer:iexchange}") String issuer) {
        this.secretKey = buildSecretKey(secret);
        this.issuer = issuer == null ? "iexchange" : issuer;
    }

    /**
     * 校验 JWT 并解析用户信息。
     *
     * @param token JWT 字符串
     * @return 解析结果，失败返回 null
     */
    public JwtUser parse(String token) {
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

    private SecretKey buildSecretKey(String secret) {
        String normalized = secret == null ? "" : secret.trim();
        if (normalized.length() < 32) {
            normalized = (normalized + "00000000000000000000000000000000").substring(0, 32);
        }
        return Keys.hmacShaKeyFor(normalized.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * JWT 解析结果。
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
