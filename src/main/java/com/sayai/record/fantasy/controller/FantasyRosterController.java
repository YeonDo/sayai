package com.sayai.record.fantasy.controller;

import com.sayai.record.auth.entity.Member;
import com.sayai.record.fantasy.dto.FantasyPlayerDto;
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

    @PostMapping("/waiver")
    public ResponseEntity<String> requestWaiver(@AuthenticationPrincipal Member member,
                                                @RequestBody WaiverRequest request) {
        if (member == null) {
            return ResponseEntity.status(401).body("Unauthorized");
        }
        fantasyRosterService.requestWaiver(request.getGameSeq(), member.getPlayerId(), request.getFantasyPlayerSeq());
        return ResponseEntity.ok("Waiver requested");
    }

    @PostMapping("/trade")
    public ResponseEntity<String> requestTrade(@AuthenticationPrincipal Member member,
                                               @RequestBody TradeRequest request) {
        if (member == null) {
            return ResponseEntity.status(401).body("Unauthorized");
        }
        fantasyRosterService.requestTrade(
                request.getGameSeq(),
                member.getPlayerId(),
                request.getTargetId(),
                request.getGivingPlayerSeqs(),
                request.getReceivingPlayerSeqs()
        );
        return ResponseEntity.ok("Trade requested");
    }

    @GetMapping("/fa")
    public ResponseEntity<List<FantasyPlayerDto>> getFAList(@RequestParam(name = "gameSeq") Long gameSeq) {
        return ResponseEntity.ok(fantasyRosterService.getFAList(gameSeq));
    }

    @PostMapping("/fa")
    public ResponseEntity<String> signFA(@AuthenticationPrincipal Member member,
                                         @RequestBody SignFARequest request) {
        if (member == null) {
            return ResponseEntity.status(401).body("Unauthorized");
        }
        fantasyRosterService.signFA(request.getGameSeq(), member.getPlayerId(), request.getFantasyPlayerSeq());
        return ResponseEntity.ok("FA Signed");
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

    @Data
    public static class SignFARequest {
        private Long gameSeq;
        private Long fantasyPlayerSeq;
    }
}
