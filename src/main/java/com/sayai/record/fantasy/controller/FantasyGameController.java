package com.sayai.record.fantasy.controller;

import com.sayai.record.auth.entity.Member;
import com.sayai.record.auth.repository.MemberRepository;
import com.sayai.record.fantasy.dto.DraftLogDto;
import com.sayai.record.fantasy.dto.FantasyGameDetailDto;
import com.sayai.record.fantasy.dto.FantasyGameDto;
import com.sayai.record.fantasy.service.FantasyGameService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/apis/v1/fantasy")
@RequiredArgsConstructor
public class FantasyGameController {

    private final FantasyGameService fantasyGameService;
    private final MemberRepository memberRepository;

    @GetMapping("/games")
    public ResponseEntity<List<FantasyGameDto>> getGames(@AuthenticationPrincipal UserDetails userDetails) {
        Long playerId = getPlayerIdFromUserDetails(userDetails);
        return ResponseEntity.ok(fantasyGameService.getDashboardGames(playerId));
    }

    @GetMapping("/my-games")
    public ResponseEntity<List<FantasyGameDto>> getMyGames(@AuthenticationPrincipal UserDetails userDetails) {
        Long playerId = getPlayerIdFromUserDetails(userDetails);
        return ResponseEntity.ok(fantasyGameService.getMyGames(playerId));
    }

    @GetMapping("/games/{gameSeq}/picks")
    public ResponseEntity<List<DraftLogDto>> getDraftPicks(@PathVariable(name = "gameSeq") Long gameSeq) {
        return ResponseEntity.ok(fantasyGameService.getDraftPicks(gameSeq));
    }

    @GetMapping("/games/{gameSeq}/details")
    public ResponseEntity<FantasyGameDetailDto> getGameDetails(@PathVariable(name = "gameSeq") Long gameSeq) {
        return ResponseEntity.ok(fantasyGameService.getGameDetails(gameSeq));
    }

    @PostMapping("/games/{gameSeq}/start")
    public ResponseEntity<String> startGame(@PathVariable(name = "gameSeq") Long gameSeq) {
        fantasyGameService.startGame(gameSeq);
        return ResponseEntity.ok("Draft Started");
    }

    private Long getPlayerIdFromUserDetails(UserDetails userDetails) {
        if (userDetails == null) {
            // For now, if no auth, maybe return default or throw 401.
            // Prototype assumption: default to 1 if not logged in, but user requirement says use login user.
            // If secure config is correct, this shouldn't be reached without auth for secured endpoints.
            // But lets throw to be safe or mock for prototype if needed.
            // Requirement: "로그인한 사용자의 player_id 를 기준으로 조회를 해야지 user_id 로 조회를 하면 안되."
            throw new IllegalArgumentException("Authentication required");
        }
        return memberRepository.findByUserId(userDetails.getUsername())
                .map(Member::getPlayerId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
    }
}
