package com.sayai.record.auth.jwt;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Collections;

@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenProvider jwtTokenProvider;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        String token = jwtTokenProvider.resolveToken(request);

        if (token != null) {
            try {
                Claims claims = jwtTokenProvider.getClaims(token);
                String userId = claims.getSubject();
                String role = claims.get("role", String.class);
                Long memberId = claims.get("memberId", Long.class);
                String name = claims.get("name", String.class);

                UserDetails userDetails = new CustomUserDetails(
                        userId,
                        "",
                        Collections.singletonList(new SimpleGrantedAuthority("ROLE_" + role)),
                        memberId,
                        name != null ? name : "Unknown"
                );

                UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(userDetails, "", userDetails.getAuthorities());
                SecurityContextHolder.getContext().setAuthentication(authentication);
            } catch (ExpiredJwtException e) {
                request.setAttribute("tokenExpired", true);
            } catch (Exception ignored) {
            }
        }
        filterChain.doFilter(request, response);
    }
}
