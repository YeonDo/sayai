package com.sayai.record.auth.controller;

import com.sayai.record.auth.jwt.CustomUserDetails;
import com.sayai.record.auth.service.AuthService;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Slf4j
@RestController
@RequestMapping("/apis/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@RequestBody LoginRequest request, HttpServletRequest httpRequest, HttpServletResponse response) {
        String ip = httpRequest.getRemoteAddr();
        String userId = request.getUserId();
        boolean fromModal = "modal".equals(httpRequest.getHeader("X-Login-Source"));
        if (fromModal) {
            log.info("[LOGIN] attempt userId={} ip={}", userId, ip);
        }
        try {
            String token = authService.login(userId, request.getPassword());
            String name = authService.getUserName(userId);

            ResponseCookie cookie = ResponseCookie.from("accessToken", token)
                    .httpOnly(true)
                    .secure(true)
                    .path("/")
                    .maxAge(21600) // 6 hours
                    .sameSite("None")
                    .build();

            response.addHeader("Set-Cookie", cookie.toString());
            if (fromModal) {
                log.info("[LOGIN] success userId={} ip={}", userId, ip);
            }
            return ResponseEntity.ok(new LoginResponse(name));
        } catch (IllegalArgumentException e) {
            if (fromModal) {
                log.warn("[LOGIN] failed userId={} ip={} reason={}", userId, ip, e.getMessage());
            }
            return ResponseEntity.status(401).build();
        }
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(HttpServletResponse response) {
        ResponseCookie cookie = ResponseCookie.from("accessToken", "")
                .httpOnly(true)
                .path("/")
                .maxAge(0) // Expire immediately
                .build();

        response.addHeader("Set-Cookie", cookie.toString());
        return ResponseEntity.ok().build();
    }

    @PostMapping("/signup")
    public ResponseEntity<String> signup(@RequestBody SignupRequest request) {
        try {
            authService.signup(request.getUserId(), request.getPassword(), request.getName(), request.getMemberId());
            return ResponseEntity.ok("User registered successfully");
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PatchMapping("/me/name")
    public ResponseEntity<String> changeName(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestBody ChangeNameRequest request) {
        if (userDetails == null) {
            return ResponseEntity.status(401).build();
        }
        try {
            authService.changeName(userDetails.getMemberId(), request.getName());
            return ResponseEntity.ok("Name changed successfully");
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PostMapping("/password")
    public ResponseEntity<String> changePassword(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestBody ChangePasswordRequest request) {
        if (userDetails == null) {
            return ResponseEntity.status(401).build();
        }
        try {
            authService.changePassword(userDetails.getMemberId(), request.getCurrentPassword(), request.getNewPassword());
            return ResponseEntity.ok("Password changed successfully");
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @GetMapping("/me")
    public ResponseEntity<UserInfo> getMe(@AuthenticationPrincipal CustomUserDetails userDetails) {
        if (userDetails == null) {
            return ResponseEntity.status(401).build();
        }
        boolean isAdmin = userDetails.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
        boolean kakaoOnly = userDetails.getUsername().startsWith("kakao_");
        return ResponseEntity.ok(new UserInfo(userDetails.getMemberId(), userDetails.getUsername(), userDetails.getName(), isAdmin, kakaoOnly));
    }

    @Data
    public static class ChangeNameRequest {
        private String name;
    }

    @Data
    public static class ChangePasswordRequest {
        private String currentPassword;
        private String newPassword;
    }

    @Data
    public static class LoginRequest {
        private String userId;
        private String password;
    }

    @Data
    public static class SignupRequest {
        private String userId;
        private String password;
        private String name;
        private Long memberId;
    }

    @Data
    @RequiredArgsConstructor
    public static class LoginResponse {
        private final String name;
    }

    @Data
    @AllArgsConstructor
    public static class UserInfo {
        private Long memberId;
        private String userId;
        private String name;
        private boolean admin;
        private boolean kakaoOnly;
    }
}
