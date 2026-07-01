package com.example.meetingappointment.config;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Base64;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

@Slf4j
@Component
public class JwtUtils {

    @Value("${jwt.secret}")
    private String secret;  // Base64 编码的密钥

    @Value("${jwt.expiration}")
    private Long expiration;

    @PostConstruct
    public void init() {
        try {
            // 验证密钥是否有效
            SecretKey key = getSignKey();
            log.info("JWT 密钥初始化成功，算法: {}", key.getAlgorithm());
            log.info("密钥长度: {} 位", key.getEncoded().length * 8);
        } catch (Exception e) {
            log.error("JWT 密钥初始化失败: {}", e.getMessage());
            throw new IllegalStateException("Invalid JWT secret key", e);
        }
    }

    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    public Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    private Claims extractAllClaims(String token) {
        return Jwts.parser()
                .verifyWith(getSignKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    private Boolean isTokenExpired(String token) {
        return extractExpiration(token).before(new Date());
    }

    public String generateToken(UserDetails userDetails) {
        Map<String, Object> claims = new HashMap<>();
        String role = userDetails.getAuthorities().iterator().next().getAuthority();
        // 去掉 ROLE_ 前缀
        if (role != null && role.startsWith("ROLE_")) {
            role = role.substring(5);
        }
        claims.put("role", role);  // 存储 ADMIN 而不是 ROLE_ADMIN
        return createToken(claims, userDetails.getUsername());
    }
    // 验证 Token（单参数版本）
    public boolean validateToken(String token) {
        try {
            return !isTokenExpired(token);
        } catch (Exception e) {
            log.error("Token 验证失败: {}", e.getMessage());
            return false;
        }
    }


    private String createToken(Map<String, Object> claims, String subject) {
        return Jwts.builder()
                .claims(claims)
                .subject(subject)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + expiration))
                .signWith(getSignKey())
                .compact();
    }

    public Boolean validateToken(String token, UserDetails userDetails) {
        final String username = extractUsername(token);
        return (username.equals(userDetails.getUsername()) && !isTokenExpired(token));
    }

    public String getRoleFromToken(String token) {
        Claims claims = extractAllClaims(token);
        String role = claims.get("role", String.class);
        // 确保返回不带前缀的角色
        if (role != null && role.startsWith("ROLE_")) {
            role = role.substring(5);
        }
        return role;
    }

    private SecretKey getSignKey() {
        // ✅ 使用 Base64 解码密钥
        byte[] keyBytes = Base64.getDecoder().decode(secret);
        return Keys.hmacShaKeyFor(keyBytes);
    }
}