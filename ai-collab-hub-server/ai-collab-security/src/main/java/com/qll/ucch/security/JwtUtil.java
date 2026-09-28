package com.qll.ucch.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

/**
 * JWT 工具类：签发 token、解析 token。
 * 密钥从配置文件读，不要写死在代码里。
 *
 * @author 人工智能学院双创平台
 */
@Slf4j
@Component
public class JwtUtil {

    /** 签名密钥，配置项：ai-collab.jwt.secret */
    @Value("${ai-collab.jwt.secret}")
    private String secret;

    /** token 有效期（毫秒），配置项：ai-collab.jwt.expire */
    @Value("${ai-collab.jwt.expire}")
    private Long expire;

    /** 从配置的字符串生成签名 key */
    private SecretKey signKey() {
        byte[] bytes = secret.getBytes(StandardCharsets.UTF_8);
        return Keys.hmacShaKeyFor(bytes);
    }

    /**
     * 签发 token。
     *
     * @param userId   用户ID
     * @param account  账号
     * @param roleId   角色ID
     * @param roleCode 角色标识
     * @return jwt 字符串
     */
    public String createToken(Long userId, String account, Long roleId, String roleCode) {
        Date now = new Date();
        Date expireAt = new Date(now.getTime() + expire);
        return Jwts.builder()
                .subject(String.valueOf(userId))
                .claim("account", account)
                .claim("roleId", roleId)
                .claim("roleCode", roleCode)
                .issuedAt(now)
                .expiration(expireAt)
                .signWith(signKey())
                .compact();
    }

    /** 解析 token，失败返回 null（过期、签名不对、乱传的都算失败） */
    public Claims parse(String token) {
        try {
            return Jwts.parser()
                    .verifyWith(signKey())
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
        } catch (Exception e) {
            log.debug("解析 token 失败：{}", e.getMessage());
            return null;
        }
    }

    /** 取用户ID，取不到返回 null */
    public Long getUserId(String token) {
        Claims claims = parse(token);
        if (claims == null) {
            return null;
        }
        try {
            return Long.valueOf(claims.getSubject());
        } catch (Exception e) {
            return null;
        }
    }

    /** 取角色ID */
    public Long getRoleId(String token) {
        Claims claims = parse(token);
        if (claims == null) {
            return null;
        }
        Object v = claims.get("roleId");
        return v == null ? null : Long.valueOf(String.valueOf(v));
    }

    /** 取角色标识 */
    public String getRoleCode(String token) {
        Claims claims = parse(token);
        if (claims == null) {
            return null;
        }
        Object v = claims.get("roleCode");
        return v == null ? null : String.valueOf(v);
    }

    /** 判断 token 是否有效 */
    public boolean valid(String token) {
        return parse(token) != null;
    }
}
