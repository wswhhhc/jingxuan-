package com.jingxuan.security;

import com.jingxuan.constant.SecurityConstants;
import io.jsonwebtoken.*;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;

/**
 * JWT Token 提供者 — 创建、解析、验证
 */
@Slf4j
@Component
public class JwtTokenProvider {

    private final SecretKey secretKey;

    @Value("${jwt.expiration:86400000}")
    private long expiration;

    @Value("${jwt.remember-expiration:604800000}")
    private long rememberExpiration;

    public JwtTokenProvider(@Value("${jwt.secret}") String secret) {
        this.secretKey = Keys.hmacShaKeyFor(Decoders.BASE64.decode(secret));
    }

    /**
     * 生成 Token
     */
    public String generateToken(Long userId, String username, String role, boolean rememberMe) {
        long exp = rememberMe ? rememberExpiration : expiration;
        return Jwts.builder()
                .claim(SecurityConstants.CLAIM_USER_ID, userId)
                .claim(SecurityConstants.CLAIM_USERNAME, username)
                .claim(SecurityConstants.CLAIM_ROLE, role)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + exp))
                .signWith(secretKey)
                .compact();
    }

    /**
     * 从 Token 中解析 Claims
     */
    public Claims parseToken(String token) {
        return Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    /**
     * 验证 Token 是否有效
     */
    public boolean validateToken(String token) {
        try {
            parseToken(token);
            return true;
        } catch (ExpiredJwtException e) {
            log.debug("Token 已过期");
        } catch (JwtException | IllegalArgumentException e) {
            log.debug("Token 无效: {}", e.getMessage());
        }
        return false;
    }

    /**
     * 从 Token 提取用户ID
     */
    public Long getUserIdFromToken(String token) {
        return parseToken(token).get(SecurityConstants.CLAIM_USER_ID, Long.class);
    }

    /**
     * 从 Token 提取用户名
     */
    public String getUsernameFromToken(String token) {
        return parseToken(token).get(SecurityConstants.CLAIM_USERNAME, String.class);
    }

    /**
     * 从 Token 提取角色
     */
    public String getRoleFromToken(String token) {
        return parseToken(token).get(SecurityConstants.CLAIM_ROLE, String.class);
    }

    /**
     * 获取 Token 剩余有效时间（毫秒）
     */
    public long getRemainingValidity(String token) {
        return parseToken(token).getExpiration().getTime() - System.currentTimeMillis();
    }
}
