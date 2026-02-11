package com.sayai.record.fantasy.controller;

import com.sayai.record.auth.jwt.CustomUserDetails;
import com.sayai.record.auth.repository.MemberRepository;
import com.sayai.record.fantasy.service.FantasyRosterService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/apis/v1/fantasy/roster")
@RequiredArgsConstructor
public class FantasyRosterController {

    private final FantasyRosterService fantasyRosterService;
    private final MemberRepository memberRepository;

    @PostMapping("/waiver")
    public ResponseEntity<String> requestWaiver(@AuthenticationPrincipal CustomUserDetails userDetails,
                                                @RequestBody WaiverRequest request) {
        if (userDetails == null) {
            return ResponseEntity.status(401).build();
        }
        fantasyRosterService.requestWaiver(request.getGameSeq(), userDetails.getPlayerId(), request.getFantasyPlayerSeq());
        return ResponseEntity.ok("Waiver requested");
    }

    @PostMapping("/trade")
    public ResponseEntity<String> requestTrade(@AuthenticationPrincipal CustomUserDetails userDetails,
                                               @RequestBody TradeRequest request) {
        if (userDetails == null) {
            return ResponseEntity.status(401).build();
        }
        fantasyRosterService.requestTrade(
                request.getGameSeq(),
                userDetails.getPlayerId(),
                request.getTargetId(),
                request.getGivingPlayerSeqs(),
                request.getReceivingPlayerSeqs()
        );
        return ResponseEntity.ok("Trade requested");
    }

    @Data
    public static class WaiverRequest {
        private Long gameSeq;
        private Long fantasyPlayerSeq;
    }

    @Data
    public static class TradeRequest {
        private Long gameSeq;
        private Long targetId;
        private List<Long> givingPlayerSeqs;
        private List<Long> receivingPlayerSeqs;
    }
}
