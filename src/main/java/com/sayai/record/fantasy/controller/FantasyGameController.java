package com.sayai.record.fantasy.controller;

import com.sayai.record.auth.entity.Member;
import com.sayai.record.auth.repository.MemberRepository;
import com.sayai.record.fantasy.dto.DraftLogDto;
import com.sayai.record.fantasy.dto.FantasyGameDetailDto;
import com.sayai.record.fantasy.dto.FantasyGameDto;
import com.sayai.record.fantasy.entity.FantasyParticipant;
import com.sayai.record.fantasy.repository.FantasyParticipantRepository;
import com.sayai.record.fantasy.service.FantasyGameService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/apis/v1/fantasy")
@RequiredArgsConstructor
public class FantasyGameController {

    private final FantasyGameService fantasyGameService;
    private final MemberRepository memberRepository;
    private final FantasyParticipantRepository fantasyParticipantRepository;

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
    public ResponseEntity<String> startGame(@PathVariable(name = "gameSeq") Long gameSeq,
                                            @AuthenticationPrincipal UserDetails userDetails) {
        boolean isAdmin = userDetails.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));

        if (!isAdmin) {
            return ResponseEntity.status(403).body("Only Admin can start the draft");
        }

        fantasyGameService.startGame(gameSeq);
        return ResponseEntity.ok("Draft Started");
    }

    @GetMapping("/games/{gameSeq}/participants")
    public ResponseEntity<List<ParticipantDto>> getGameParticipants(@PathVariable(name = "gameSeq") Long gameSeq) {
        List<FantasyParticipant> participants = fantasyParticipantRepository.findByFantasyGameSeq(gameSeq);

        java.util.Set<Long> playerIds = participants.stream()
                .map(FantasyParticipant::getPlayerId)
                .filter(java.util.Objects::nonNull)
                .collect(Collectors.toSet());

        Map<Long, String> memberNames = memberRepository.findAllById(playerIds).stream()
                .collect(Collectors.toMap(Member::getPlayerId, m -> m.getName() != null ? m.getName() : "Unknown"));

        List<ParticipantDto> dtos = participants.stream().map(p -> {
            String userName = memberNames.getOrDefault(p.getPlayerId(), "Unknown");
            return new ParticipantDto(p.getSeq(), p.getPlayerId(), userName, p.getTeamName(), p.getPreferredTeam());
        }).collect(Collectors.toList());
        return ResponseEntity.ok(dtos);
    }

    private Long getPlayerIdFromUserDetails(UserDetails userDetails) {
        if (userDetails == null) {
            throw new IllegalArgumentException("Authentication required");
        }
        return memberRepository.findByUserId(userDetails.getUsername())
                .map(Member::getPlayerId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
    }

    @Data
    public static class ParticipantDto {
        private Long seq;
        private Long playerId;
        private String userName;
        private String teamName;
        private String preferredTeam;

        public ParticipantDto(Long seq, Long playerId, String userName, String teamName, String preferredTeam) {
            this.seq = seq;
            this.playerId = playerId;
            this.userName = userName;
            this.teamName = teamName;
            this.preferredTeam = preferredTeam;
        }
    }
}
