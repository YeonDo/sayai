package com.sayai.record.fantasy.controller;

import com.sayai.record.auth.entity.Member;
import com.sayai.record.auth.repository.MemberRepository;
import com.sayai.record.fantasy.dto.DraftRequest;
import com.sayai.record.fantasy.dto.FantasyPlayerDto;
import com.sayai.record.fantasy.service.FantasyDraftService;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/apis/v1/fantasy")
@RequiredArgsConstructor
public class FantasyDraftController {

    private final FantasyDraftService fantasyDraftService;
    private final MemberRepository memberRepository;

    @GetMapping("/games/{gameSeq}/available-players")
    public ResponseEntity<List<FantasyPlayerDto>> getAvailablePlayers(
            @PathVariable(name = "gameSeq") Long gameSeq,
            @RequestParam(name = "team", required = false) String team,
            @RequestParam(name = "position", required = false) String position,
            @RequestParam(name = "search", required = false) String search) {
        return ResponseEntity.ok(fantasyDraftService.getAvailablePlayers(gameSeq, team, position, search));
    }

    @PostMapping("/games/{gameSeq}/join")
    public ResponseEntity<String> joinGame(@PathVariable(name = "gameSeq") Long gameSeq,
                                           @RequestBody JoinRequest request,
                                           @AuthenticationPrincipal UserDetails userDetails) {
        try {
            Long playerId = getPlayerIdFromUserDetails(userDetails);
            // Ignore request.playerId if passed, use authenticated ID
            fantasyDraftService.joinGame(gameSeq, playerId, request.getPreferredTeam(), request.getTeamName());
            return ResponseEntity.ok("Joined successfully");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PostMapping("/draft")
    public ResponseEntity<String> draftPlayer(@RequestBody DraftRequest request,
                                              @AuthenticationPrincipal UserDetails userDetails) {
        try {
            Long playerId = getPlayerIdFromUserDetails(userDetails);
            if (!playerId.equals(request.getPlayerId())) {
                 // Or just override
                 request.setPlayerId(playerId);
            }
            fantasyDraftService.draftPlayer(request);
            return ResponseEntity.ok("Draft successful");
        } catch (IllegalArgumentException | IllegalStateException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Draft failed: " + e.getMessage());
        }
    }

    @GetMapping("/games/{gameSeq}/players/{playerId}/picks")
    public ResponseEntity<List<FantasyPlayerDto>> getPickedPlayers(
            @PathVariable(name = "gameSeq") Long gameSeq,
            @PathVariable(name = "playerId") Long playerId) {
        return ResponseEntity.ok(fantasyDraftService.getPickedPlayers(gameSeq, playerId));
    }

    @GetMapping("/games/{gameSeq}/my-picks")
    public ResponseEntity<List<FantasyPlayerDto>> getMyPicks(
            @PathVariable(name = "gameSeq") Long gameSeq,
            @AuthenticationPrincipal UserDetails userDetails) {
        Long playerId = getPlayerIdFromUserDetails(userDetails);
        return ResponseEntity.ok(fantasyDraftService.getPickedPlayers(gameSeq, playerId));
    }

    private Long getPlayerIdFromUserDetails(UserDetails userDetails) {
        if (userDetails == null) {
            throw new IllegalArgumentException("Authentication required");
        }
        return memberRepository.findByUserId(userDetails.getUsername())
                .map(Member::getPlayerId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
    }

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class JoinRequest {
        private Long playerId; // Optional now
        private String preferredTeam;
        private String teamName;
    }
}
