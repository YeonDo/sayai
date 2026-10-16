package com.sayai.record.point;

import com.sayai.record.auth.jwt.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/apis/v1/point")
@RequiredArgsConstructor
public class PointController {

    private final PointService pointService;

    @GetMapping("/me")
    public ResponseEntity<Map<String, Object>> myPoint(@AuthenticationPrincipal CustomUserDetails userDetails) {
        if (userDetails == null) {
            return ResponseEntity.status(401).build();
        }
        return ResponseEntity.ok(Map.of(
                "memberId", userDetails.getMemberId(),
                "balance", pointService.getBalance(userDetails.getMemberId())));
    }
}
