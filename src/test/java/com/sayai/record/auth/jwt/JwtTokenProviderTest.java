package com.sayai.record.auth.jwt;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.security.Key;
import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;

class JwtTokenProviderTest {

    private JwtTokenProvider jwtTokenProvider;
    private Key secretKey;

    @BeforeEach
    void setUp() {
        jwtTokenProvider = new JwtTokenProvider();
        secretKey = Keys.secretKeyFor(SignatureAlgorithm.HS256);
        ReflectionTestUtils.setField(jwtTokenProvider, "secretKey", secretKey);
    }

    @Test
    void createToken_shouldReturnValidToken() {
        String token = jwtTokenProvider.createToken(1L, "user");
        assertThat(token).isNotNull();
        assertThat(jwtTokenProvider.validateToken(token)).isTrue();
    }

    @Test
    void validateToken_shouldReturnFalse_whenExpired() {
        // Manually create expired token
        String token = Jwts.builder()
                .setSubject("user")
                .setExpiration(new Date(System.currentTimeMillis() - 1000))
                .signWith(secretKey, SignatureAlgorithm.HS256)
                .compact();

        assertThat(jwtTokenProvider.validateToken(token)).isFalse();
    }
}
