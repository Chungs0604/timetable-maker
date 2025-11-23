package com.timetable.common.jwt;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;

@Service
public class JwtService {

    private final byte[] keyBytes;
    private final long   expires;

    public JwtService(
            @Value("${jwt.secret}") String secret,
            @Value("${jwt.expires-seconds}") long expiresSeconds
    ) {
        this.keyBytes = secret.getBytes(StandardCharsets.UTF_8);
        this.expires  = expiresSeconds;
    }

    /*임* 액세스 토큰 발급: sub=userId, email 클레 포함 */
    public String issue(Long userId, String email) {
        Instant now = Instant.now();
        return Jwts.builder()
                .setSubject(String.valueOf(userId))
                .claim("email", email)
                .setIssuedAt(Date.from(now))
                .setExpiration(Date.from(now.plusSeconds(expires)))
                .signWith(Keys.hmacShaKeyFor(keyBytes), SignatureAlgorithm.HS256)
                .compact();
    }

    /** 유효성 검사 + 클레임 파싱 (필요 시 try-catch 호출부에서 처리) */
    public Jws<Claims> parse(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(Keys.hmacShaKeyFor(keyBytes))
                .build()
                .parseClaimsJws(token);
    }

    /** 편의 메서드들 */
    public long getExpiresSeconds() { return expires; }
    public Long getUserId(String token) { return Long.valueOf(parse(token).getBody().getSubject()); }
    public String getEmail(String token) { return parse(token).getBody().get("email", String.class); }
}
