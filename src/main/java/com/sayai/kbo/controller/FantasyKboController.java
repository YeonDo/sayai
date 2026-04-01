package com.sayai.kbo.controller;

import com.sayai.kbo.dto.ParticipantKboStatsDto;
import com.sayai.kbo.service.FantasyKboService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/apis/v1/fantasy/games/{gameSeq}/kbo-stats")
@RequiredArgsConstructor
public class FantasyKboController {

    private final FantasyKboService fantasyKboService;

    @GetMapping
    public ResponseEntity<List<ParticipantKboStatsDto>> getParticipantStats(
            @PathVariable("gameSeq") Long gameSeq,
            @RequestParam("startDt") @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate startDt,
            @RequestParam("endDt") @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate endDt) {

        List<ParticipantKboStatsDto> stats = fantasyKboService.aggregateStats(gameSeq, startDt, endDt);
        return ResponseEntity.ok(stats);
    }
}
