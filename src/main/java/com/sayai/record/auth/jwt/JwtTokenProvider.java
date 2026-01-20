package com.sayai.record.auth.jwt;

import com.sayai.record.auth.entity.Member;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import java.security.Key;
import java.util.Arrays;
import java.util.Date;

@Component
@RequiredArgsConstructor
public class JwtTokenProvider {

    private Key secretKey;
    private final long tokenValidityInMilliseconds = 3600000; // 1h

    @PostConstruct
    protected void init() {
        // Generate a secure key for HS256
        this.secretKey = Keys.secretKeyFor(SignatureAlgorithm.HS256);
    }

    public String createToken(Long playerId, String userId, Member.Role role) {
        Claims claims = Jwts.claims().setSubject(userId);
        claims.put("playerId", playerId);
        claims.put("role", role.name());

        Date now = new Date();
        Date validity = new Date(now.getTime() + tokenValidityInMilliseconds);

        return Jwts.builder()
                .setClaims(claims)
                .setIssuedAt(now)
                .setExpiration(validity)
                .signWith(secretKey, SignatureAlgorithm.HS256)
                .compact();
    }

    public String resolveToken(HttpServletRequest request) {
        // 1. Try Cookie first
        if (request.getCookies() != null) {
            return Arrays.stream(request.getCookies())
                    .filter(c -> "accessToken".equals(c.getName()))
                    .map(Cookie::getValue)
                    .findFirst()
                    .orElse(null);
        }
        // 2. Fallback to Header (optional, good for API clients like Postman)
        String bearerToken = request.getHeader("Authorization");
        if (bearerToken != null && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }

    public boolean validateToken(String token) {
        try {
            Jws<Claims> claims = Jwts.parserBuilder().setSigningKey(secretKey).build().parseClaimsJws(token);
            return !claims.getBody().getExpiration().before(new Date());
        } catch (Exception e) {
            return false;
        }
    }

    public String getUserId(String token) {
        return Jwts.parserBuilder().setSigningKey(secretKey).build().parseClaimsJws(token).getBody().getSubject();
    }

    public Long getPlayerId(String token) {
        return Jwts.parserBuilder().setSigningKey(secretKey).build().parseClaimsJws(token).getBody().get("playerId", Long.class);
    }

    public String getRole(String token) {
        return Jwts.parserBuilder().setSigningKey(secretKey).build().parseClaimsJws(token).getBody().get("role", String.class);
    }
}
