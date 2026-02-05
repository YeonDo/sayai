package com.sayai.record.auth.config;

import com.sayai.record.auth.jwt.JwtAuthenticationFilter;
import com.sayai.record.auth.jwt.JwtTokenProvider;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtTokenProvider jwtTokenProvider;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .httpBasic(AbstractHttpConfigurer::disable)
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        // Allow public access to Record APIs and Auth Login
                        .requestMatchers("/apis/v1/player/**", "/apis/v1/auth/login", "/apis/v1/auth/signup","/apis/v1/fantasy/players/import", "/").permitAll()
                        .requestMatchers("/css/**", "/js/**", "/images/**", "/favicon.ico").permitAll()
                        // Admin access
                        .requestMatchers("/apis/v1/admin/**").hasRole("ADMIN")
                        // Require auth for Fantasy APIs and Views
                        .requestMatchers("/apis/v1/fantasy/**", "/fantasy/**").authenticated()
                        .requestMatchers("/apis/v1/auth/password").authenticated()
                        .anyRequest().permitAll()
                )
                .exceptionHandling(e -> e
                        .authenticationEntryPoint((request, response, authException) -> {
                            String uri = request.getRequestURI();
                            if (uri.startsWith("/apis/")) {
                                response.sendError(HttpServletResponse.SC_FORBIDDEN);
                            } else {
                                String redirectUrl = "/?login=required";
                                if (!"/".equals(uri)) {
                                    redirectUrl += "&redirect=" + URLEncoder.encode(uri, StandardCharsets.UTF_8);
                                }
                                response.sendRedirect(redirectUrl);
                            }
                        })
                )
                .addFilterBefore(new JwtAuthenticationFilter(jwtTokenProvider), UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
