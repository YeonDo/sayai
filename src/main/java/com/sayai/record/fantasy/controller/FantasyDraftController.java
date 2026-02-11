package com.sayai.record.fantasy.controller;

import com.sayai.record.auth.jwt.CustomUserDetails;
import com.sayai.record.auth.repository.MemberRepository;
import com.sayai.record.fantasy.dto.DraftRequest;
import com.sayai.record.fantasy.dto.FantasyPlayerDto;
import com.sayai.record.fantasy.dto.RosterUpdateDto;
import com.sayai.record.fantasy.service.FantasyDraftService;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
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
            @RequestParam(name = "search", required = false) String search,
            @RequestParam(name = "sort", required = false) String sort,
            @RequestParam(name = "foreignerType", required = false) String foreignerType) {
        return ResponseEntity.ok(fantasyDraftService.getAvailablePlayers(gameSeq, team, position, search, sort, foreignerType));
    }

    @PostMapping("/games/{gameSeq}/join")
    public ResponseEntity<String> joinGame(@PathVariable(name = "gameSeq") Long gameSeq,
                                           @RequestBody JoinRequest request,
                                           @AuthenticationPrincipal CustomUserDetails userDetails) {
        if (userDetails == null) {
            return ResponseEntity.status(401).build();
        }
        try {
            // Ignore request.playerId if passed, use authenticated ID
            fantasyDraftService.joinGame(gameSeq, userDetails.getPlayerId(), request.getPreferredTeam(), request.getTeamName());
            return ResponseEntity.ok("Joined successfully");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PostMapping("/draft")
    public ResponseEntity<String> draftPlayer(@RequestBody DraftRequest request,
                                              @AuthenticationPrincipal CustomUserDetails userDetails) {
        if (userDetails == null) {
            return ResponseEntity.status(401).build();
        }
        try {
            if (!userDetails.getPlayerId().equals(request.getPlayerId())) {
                 // Or just override
                 request.setPlayerId(userDetails.getPlayerId());
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
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        if (userDetails == null) {
            return ResponseEntity.status(401).build();
        }
        return ResponseEntity.ok(fantasyDraftService.getPickedPlayers(gameSeq, userDetails.getPlayerId()));
    }

    @PostMapping("/games/{gameSeq}/my-team/save")
    public ResponseEntity<String> saveRoster(@PathVariable(name = "gameSeq") Long gameSeq,
                                             @RequestBody RosterUpdateDto request,
                                             @AuthenticationPrincipal CustomUserDetails userDetails) {
        if (userDetails == null) {
            return ResponseEntity.status(401).build();
        }
        try {
            fantasyDraftService.updateRoster(gameSeq, userDetails.getPlayerId(), request);
            return ResponseEntity.ok("Roster saved");
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Save failed: " + e.getMessage());
        }
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
