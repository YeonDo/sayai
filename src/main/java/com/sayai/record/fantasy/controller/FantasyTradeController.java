package com.sayai.record.fantasy.controller;

import com.sayai.record.auth.entity.Member;
import com.sayai.record.auth.repository.MemberRepository;
import com.sayai.record.fantasy.dto.TradeProposalDto;
import com.sayai.record.fantasy.service.FantasyTradeService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/apis/v1/fantasy/trade")
@RequiredArgsConstructor
public class FantasyTradeController {

    private final FantasyTradeService fantasyTradeService;
    private final MemberRepository memberRepository;

    @PostMapping("/waiver/drop")
    public ResponseEntity<String> dropPlayer(@RequestBody DropRequest request,
                                             @AuthenticationPrincipal UserDetails userDetails) {
        try {
            Long playerId = getPlayerIdFromUserDetails(userDetails);
            fantasyTradeService.dropPlayer(request.getGameSeq(), playerId, request.getFantasyPlayerSeq());
            return ResponseEntity.ok("Player dropped successfully");
        } catch (IllegalStateException | IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Error: " + e.getMessage());
        }
    }

    @PostMapping("/waiver/claim")
    public ResponseEntity<String> claimPlayer(@RequestBody DropRequest request,
                                              @AuthenticationPrincipal UserDetails userDetails) {
        try {
            Long playerId = getPlayerIdFromUserDetails(userDetails);
            fantasyTradeService.claimPlayer(request.getGameSeq(), playerId, request.getFantasyPlayerSeq());
            return ResponseEntity.ok("Player claimed successfully");
        } catch (IllegalStateException | IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Error: " + e.getMessage());
        }
    }

    @PostMapping("/propose")
    public ResponseEntity<String> proposeTrade(@RequestBody TradeProposalDto request,
                                               @AuthenticationPrincipal UserDetails userDetails) {
        try {
            Long playerId = getPlayerIdFromUserDetails(userDetails);
            fantasyTradeService.proposeTrade(playerId, request);
            return ResponseEntity.ok("Trade proposed successfully");
        } catch (IllegalStateException | IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Error: " + e.getMessage());
        }
    }

    private Long getPlayerIdFromUserDetails(UserDetails userDetails) {
        if (userDetails == null) {
            throw new IllegalArgumentException("Authentication required");
        }
        return memberRepository.findByUserId(userDetails.getUsername())
                .map(Member::getPlayerId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
    }

    public static class DropRequest {
        private Long gameSeq;
        private Long fantasyPlayerSeq;

        public Long getGameSeq() { return gameSeq; }
        public void setGameSeq(Long gameSeq) { this.gameSeq = gameSeq; }
        public Long getFantasyPlayerSeq() { return fantasyPlayerSeq; }
        public void setFantasyPlayerSeq(Long fantasyPlayerSeq) { this.fantasyPlayerSeq = fantasyPlayerSeq; }
    }
}
