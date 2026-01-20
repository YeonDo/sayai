package com.sayai.record.admin.controller;

import com.sayai.record.auth.entity.Member;
import com.sayai.record.auth.repository.MemberRepository;
import com.sayai.record.fantasy.entity.FantasyGame;
import com.sayai.record.fantasy.repository.FantasyGameRepository;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/apis/v1/admin")
@RequiredArgsConstructor
public class AdminController {

    private final FantasyGameRepository fantasyGameRepository;
    private final MemberRepository memberRepository;

    @PostMapping("/games")
    public ResponseEntity<FantasyGame> createGame(@RequestBody GameCreateRequest request) {
        FantasyGame game = FantasyGame.builder()
                .title(request.getTitle())
                .ruleType(request.getRuleType())
                .scoringType(request.getScoringType())
                .scoringSettings(request.getScoringSettings())
                .status(FantasyGame.GameStatus.WAITING)
                .build();

        return ResponseEntity.ok(fantasyGameRepository.save(game));
    }

    @GetMapping("/users")
    public ResponseEntity<List<Member>> listUsers() {
        return ResponseEntity.ok(memberRepository.findAll());
    }

    @PutMapping("/users/{id}/role")
    public ResponseEntity<String> updateUserRole(@PathVariable Long id, @RequestParam Member.Role role) {
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
    }
}
