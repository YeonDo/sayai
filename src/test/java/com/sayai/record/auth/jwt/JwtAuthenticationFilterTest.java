package com.sayai.record.auth.jwt;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.security.core.context.SecurityContextHolder;

import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class JwtAuthenticationFilterTest {

    @Mock
    private JwtTokenProvider jwtTokenProvider;

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @Mock
    private FilterChain filterChain;

    @InjectMocks
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        SecurityContextHolder.clearContext();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void doFilterInternal_validToken_setsAuthentication() throws Exception {
        String token = "valid-token";
        String userId = "testUser";
        String role = "USER";
        Long playerId = 123L;

        when(jwtTokenProvider.resolveToken(request)).thenReturn(token);
        when(jwtTokenProvider.validateToken(token)).thenReturn(true);
        String name = "Test Name";
        when(jwtTokenProvider.getUserId(token)).thenReturn(userId);
        when(jwtTokenProvider.getRole(token)).thenReturn(role);
        when(jwtTokenProvider.getPlayerId(token)).thenReturn(playerId);
        when(jwtTokenProvider.getName(token)).thenReturn(name);

        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);

        assertNotNull(SecurityContextHolder.getContext().getAuthentication());
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        assertTrue(principal instanceof com.sayai.record.auth.entity.Member);
        com.sayai.record.auth.entity.Member member = (com.sayai.record.auth.entity.Member) principal;
        assertEquals(userId, member.getUserId());
        assertEquals(playerId, member.getPlayerId());
        assertEquals(name, member.getName());
    }

    @Test
    void doFilterInternal_invalidToken_doesNotSetAuthentication() throws Exception {
        String token = "invalid-token";

        when(jwtTokenProvider.resolveToken(request)).thenReturn(token);
        when(jwtTokenProvider.validateToken(token)).thenReturn(false);

        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }
}
