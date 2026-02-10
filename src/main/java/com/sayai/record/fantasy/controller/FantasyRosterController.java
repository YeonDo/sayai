package com.sayai.record.fantasy.controller;

import com.sayai.record.auth.entity.Member;
import com.sayai.record.auth.repository.MemberRepository;
import com.sayai.record.fantasy.service.FantasyRosterService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/apis/v1/fantasy/roster")
@RequiredArgsConstructor
public class FantasyRosterController {

    private final FantasyRosterService fantasyRosterService;
    private final MemberRepository memberRepository;

    @PostMapping("/waiver")
    public ResponseEntity<String> requestWaiver(@AuthenticationPrincipal UserDetails userDetails,
                                                @RequestBody WaiverRequest request) {
        Long playerId = getPlayerIdFromUserDetails(userDetails);
        fantasyRosterService.requestWaiver(request.getGameSeq(), playerId, request.getFantasyPlayerSeq());
        return ResponseEntity.ok("Waiver requested");
    }

    @PostMapping("/trade")
    public ResponseEntity<String> requestTrade(@AuthenticationPrincipal UserDetails userDetails,
                                               @RequestBody TradeRequest request) {
        Long playerId = getPlayerIdFromUserDetails(userDetails);
        fantasyRosterService.requestTrade(
                request.getGameSeq(),
                playerId,
                request.getTargetId(),
                request.getGivingPlayerSeqs(),
                request.getReceivingPlayerSeqs()
        );
        return ResponseEntity.ok("Trade requested");
    }

    private Long getPlayerIdFromUserDetails(UserDetails userDetails) {
        if (userDetails == null) {
            throw new IllegalArgumentException("Authentication required");
        }
        return memberRepository.findByUserId(userDetails.getUsername())
                .map(Member::getPlayerId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
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
