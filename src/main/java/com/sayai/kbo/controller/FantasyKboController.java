package com.sayai.kbo.controller;

import com.sayai.kbo.dto.MyRosterStatDto;
import com.sayai.kbo.dto.ParticipantKboStatsDto;
import com.sayai.kbo.service.FantasyKboService;
import com.sayai.record.auth.jwt.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/apis/v1/fantasy/games/{gameSeq}")
@RequiredArgsConstructor
public class FantasyKboController {

    private final FantasyKboService fantasyKboService;

    @GetMapping("/kbo-stats")
    public ResponseEntity<List<ParticipantKboStatsDto>> getParticipantStats(
            @PathVariable("gameSeq") Long gameSeq,
            @RequestParam("startDt") @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate startDt,
            @RequestParam("endDt") @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate endDt) {

        List<ParticipantKboStatsDto> stats = fantasyKboService.aggregateStats(gameSeq, startDt, endDt);
        return ResponseEntity.ok(stats);
    }

    @GetMapping("/my-roster/stats")
    public ResponseEntity<MyRosterStatDto> getMyRosterStats(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable("gameSeq") Long gameSeq,
            @RequestParam("startDt") @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate startDt,
            @RequestParam("endDt") @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate endDt) {

        if (userDetails == null) {
            return ResponseEntity.status(401).build();
        }
        return ResponseEntity.ok(fantasyKboService.getMyRosterStats(gameSeq, userDetails.getPlayerId(), startDt, endDt));
    }
}
