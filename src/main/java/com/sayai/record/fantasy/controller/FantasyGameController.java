package com.sayai.record.fantasy.controller;

import com.sayai.record.fantasy.dto.DraftLogDto;
import com.sayai.record.fantasy.dto.FantasyGameDto;
import com.sayai.record.fantasy.service.FantasyGameService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/apis/v1/fantasy")
@RequiredArgsConstructor
public class FantasyGameController {

    private final FantasyGameService fantasyGameService;

    @GetMapping("/games")
    public ResponseEntity<List<FantasyGameDto>> getGames(@RequestParam(required = false, defaultValue = "1") Long userId) {
        return ResponseEntity.ok(fantasyGameService.getDashboardGames(userId));
    }

    @GetMapping("/my-games")
    public ResponseEntity<List<FantasyGameDto>> getMyGames(@RequestParam(required = false, defaultValue = "1") Long userId) {
        return ResponseEntity.ok(fantasyGameService.getMyGames(userId));
    }

    @GetMapping("/games/{gameSeq}/picks")
    public ResponseEntity<List<DraftLogDto>> getDraftPicks(@PathVariable Long gameSeq) {
        return ResponseEntity.ok(fantasyGameService.getDraftPicks(gameSeq));
    }
}
