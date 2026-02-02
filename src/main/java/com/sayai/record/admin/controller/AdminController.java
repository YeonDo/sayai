package com.sayai.record.admin.controller;

import com.sayai.record.auth.entity.Member;
import com.sayai.record.auth.repository.MemberRepository;
import com.sayai.record.fantasy.entity.FantasyGame;
import com.sayai.record.fantasy.service.FantasyGameService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/apis/v1/admin")
@RequiredArgsConstructor
public class AdminController {

    private final FantasyGameService fantasyGameService;
    private final MemberRepository memberRepository;

    @PostMapping("/games")
    public ResponseEntity<FantasyGame> createGame(@RequestBody GameCreateRequest request) {
        FantasyGame game = fantasyGameService.createGame(
                request.getTitle(),
                request.getRuleType(),
                request.getScoringType(),
                request.getScoringSettings(),
                request.getMaxParticipants(),
                request.getDraftDate(),
                request.getGameDuration(),
                request.getDraftTimeLimit(),
                request.getUseFirstPickRule(),
                request.getSalaryCap(),
                request.getUseTeamRestriction()
        );

        return ResponseEntity.ok(game);
    }

    @PutMapping("/fantasy/games/{gameSeq}/status")
    public ResponseEntity<Void> updateGameStatus(@PathVariable(name = "gameSeq") Long gameSeq,
                                                 @RequestParam(name = "status") FantasyGame.GameStatus status) {
        fantasyGameService.updateGameStatus(gameSeq, status);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/users")
    public ResponseEntity<List<Member>> listUsers() {
        return ResponseEntity.ok(memberRepository.findAll());
    }

    @PutMapping("/users/{id}/role")
    public ResponseEntity<String> updateUserRole(@PathVariable(name = "id") Long id,
                                                 @RequestParam(name = "role") Member.Role role) {
        Member member = memberRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        member.setRole(role);
        memberRepository.save(member);

        return ResponseEntity.ok("Role updated to " + role);
    }

    @Data
    public static class GameCreateRequest {
        private String title;
        private FantasyGame.RuleType ruleType;
        private FantasyGame.ScoringType scoringType;
        private String scoringSettings;
        private Integer maxParticipants;
        private LocalDateTime draftDate;
        private Integer draftTimeLimit;
        private String gameDuration;
        private Boolean useFirstPickRule;
        private Integer salaryCap;
        private Boolean useTeamRestriction;
    }
}
