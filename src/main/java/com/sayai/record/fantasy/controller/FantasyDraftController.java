package com.sayai.record.fantasy.controller;

import com.sayai.record.fantasy.dto.DraftRequest;
import com.sayai.record.fantasy.dto.FantasyPlayerDto;
import com.sayai.record.fantasy.service.FantasyDraftService;
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
    public ResponseEntity<List<FantasyPlayerDto>> getAvailablePlayers(@PathVariable Long gameSeq) {
        return ResponseEntity.ok(fantasyDraftService.getAvailablePlayers(gameSeq));
    }

    @PostMapping("/draft")
    public ResponseEntity<String> draftPlayer(@RequestBody DraftRequest request) {
        try {
            fantasyDraftService.draftPlayer(request);
            return ResponseEntity.ok("Draft successful");
        } catch (IllegalArgumentException | IllegalStateException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Draft failed");
        }
    }

    @GetMapping("/games/{gameSeq}/players/{playerId}/picks")
    public ResponseEntity<List<FantasyPlayerDto>> getPickedPlayers(
            @PathVariable Long gameSeq,
            @PathVariable Long playerId) {
        return ResponseEntity.ok(fantasyDraftService.getPickedPlayers(gameSeq, playerId));
    }
}
