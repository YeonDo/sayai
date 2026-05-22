package com.sayai.record.auth.controller;

import com.sayai.record.auth.jwt.CustomUserDetails;
import com.sayai.record.auth.service.KakaoOAuthService;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/apis/v1/auth/kakao")
@RequiredArgsConstructor
public class KakaoOAuthController {

    private final KakaoOAuthService kakaoOAuthService;

    @PostMapping("/login")
    public ResponseEntity<Map<String, String>> login(@RequestParam String code, HttpServletResponse response) {
        KakaoOAuthService.LoginResult result = kakaoOAuthService.login(code);
        ResponseCookie cookie = ResponseCookie.from("accessToken", result.token())
                .httpOnly(true)
                .secure(true)
                .path("/")
                .maxAge(21600)
                .sameSite("None")
                .build();
        response.addHeader("Set-Cookie", cookie.toString());
        return ResponseEntity.ok(Map.of("name", result.name()));
    }

    @GetMapping("/status")
    public ResponseEntity<Map<String, Boolean>> status(@AuthenticationPrincipal CustomUserDetails userDetails) {
        boolean linked = kakaoOAuthService.getLinkStatus(userDetails.getMemberId());
        return ResponseEntity.ok(Map.of("linked", linked));
    }

    @PostMapping("/link")
    public ResponseEntity<String> link(@RequestParam String code,
                                       @AuthenticationPrincipal CustomUserDetails userDetails) {
        kakaoOAuthService.linkAccount(userDetails.getMemberId(), code);
        return ResponseEntity.ok("카카오 계정이 연동되었습니다.");
    }

    @DeleteMapping("/unlink")
    public ResponseEntity<String> unlink(@AuthenticationPrincipal CustomUserDetails userDetails) {
        kakaoOAuthService.unlinkAccount(userDetails.getMemberId());
        return ResponseEntity.ok("카카오 연동이 해제되었습니다.");
    }
}
