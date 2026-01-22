package com.sayai.record.fantasy.controller;

import com.sayai.record.fantasy.dto.DraftRequest;
import com.sayai.record.fantasy.dto.FantasyPlayerDto;
import com.sayai.record.fantasy.service.FantasyDraftService;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/apis/v1/fantasy")
@RequiredArgsConstructor
public class FantasyDraftController {

    private final FantasyDraftService fantasyDraftService;

    @GetMapping("/games/{gameSeq}/available-players")
    public ResponseEntity<List<FantasyPlayerDto>> getAvailablePlayers(
            @PathVariable("gameSeq") Long gameSeq,
            @RequestParam(name = "team", required = false) String team,
            @RequestParam(name = "position", required = false) String position,
            @RequestParam(name = "search", required = false) String search) {
        return ResponseEntity.ok(fantasyDraftService.getAvailablePlayers(gameSeq, team, position, search));
    }

    @PostMapping("/games/{gameSeq}/join")
    public ResponseEntity<String> joinGame(@PathVariable("gameSeq") Long gameSeq, @RequestBody JoinRequest request) {
        try {
            fantasyDraftService.joinGame(gameSeq, request.getPlayerId(), request.getPreferredTeam());
            return ResponseEntity.ok("Joined successfully");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PostMapping("/draft")
    public ResponseEntity<String> draftPlayer(@RequestBody DraftRequest request) {
        try {
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
            @PathVariable("gameSeq") Long gameSeq,
            @PathVariable("playerId") Long playerId) {
        return ResponseEntity.ok(fantasyDraftService.getPickedPlayers(gameSeq, playerId));
    }

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class JoinRequest {
        private Long playerId;
        private String preferredTeam;
    }
}
