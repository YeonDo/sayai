package com.sayai.record.admin.controller;

import com.sayai.record.auth.entity.Member;
import com.sayai.record.auth.repository.MemberRepository;
import com.sayai.record.auth.service.AuthService;
import com.sayai.record.fantasy.dto.FantasyScoreDto;
import com.sayai.record.fantasy.entity.FantasyGame;
import com.sayai.record.fantasy.entity.FantasyParticipant;
import com.sayai.record.fantasy.repository.FantasyParticipantRepository;
import com.sayai.record.fantasy.service.FantasyGameService;
import com.sayai.record.fantasy.service.FantasyScoringService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.ToString;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/apis/v1/admin")
@RequiredArgsConstructor
public class AdminController {

    private final FantasyGameService fantasyGameService;
    private final MemberRepository memberRepository;
    private final FantasyParticipantRepository fantasyParticipantRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthService authService;
    private final FantasyScoringService fantasyScoringService;

    // --- Game Management ---

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
                request.getUseTeamRestriction(),
                request.getRounds()
        );

        return ResponseEntity.ok(game);
    }

    @PutMapping("/fantasy/games/{gameSeq}/status")
    public ResponseEntity<Void> updateGameStatus(@PathVariable(name = "gameSeq") Long gameSeq,
                                                 @RequestParam(name = "status") FantasyGame.GameStatus status) {
        fantasyGameService.updateGameStatus(gameSeq, status);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/fantasy/games/{gameSeq}/participants")
    public ResponseEntity<List<ParticipantDto>> getGameParticipants(@PathVariable(name = "gameSeq") Long gameSeq) {
        List<FantasyParticipant> participants = fantasyParticipantRepository.findByFantasyGameSeq(gameSeq);

        java.util.Set<Long> playerIds = participants.stream()
                .map(FantasyParticipant::getPlayerId)
                .filter(java.util.Objects::nonNull)
                .collect(Collectors.toSet());

        Map<Long, String> memberNames = memberRepository.findAllById(playerIds).stream()
                .collect(Collectors.toMap(Member::getPlayerId, Member::getName));

        List<ParticipantDto> dtos = participants.stream().map(p -> {
            String userName = memberNames.getOrDefault(p.getPlayerId(), "Unknown");
            return new ParticipantDto(p.getSeq(), p.getPlayerId(), userName, p.getTeamName(), p.getPreferredTeam());
        }).collect(Collectors.toList());
        return ResponseEntity.ok(dtos);
    }

    @PutMapping("/fantasy/participants/{seq}")
    public ResponseEntity<String> updateParticipantTeamName(@PathVariable(name = "seq") Long seq,
                                                            @RequestBody Map<String, String> body) {
        FantasyParticipant participant = fantasyParticipantRepository.findById(seq)
                .orElseThrow(() -> new IllegalArgumentException("Participant not found"));

        if (body.containsKey("teamName")) {
            participant.setTeamName(body.get("teamName"));
            fantasyParticipantRepository.save(participant);
        }
        return ResponseEntity.ok("Updated");
    }

    @GetMapping("/fantasy/games/{gameSeq}/export-players")
    public ResponseEntity<String> exportPlayers(@PathVariable(name = "gameSeq") Long gameSeq) {
        String data = fantasyGameService.exportRoster(gameSeq);
        return ResponseEntity.ok(data);
    }

    // --- Scoring Endpoints ---

    @GetMapping("/fantasy/games/{gameSeq}/scores/{round}")
    public ResponseEntity<List<FantasyScoreDto>> getScores(@PathVariable(name = "gameSeq") Long gameSeq,
                                                           @PathVariable(name = "round") Integer round) {
        return ResponseEntity.ok(fantasyScoringService.getScores(gameSeq, round));
    }

    @PostMapping("/fantasy/games/{gameSeq}/scores/{round}")
    public ResponseEntity<String> saveScores(@PathVariable(name = "gameSeq") Long gameSeq,
                                             @PathVariable(name = "round") Integer round,
                                             @RequestBody List<FantasyScoreDto> scores) {
        fantasyScoringService.saveAndCalculateScores(gameSeq, round, scores);
        return ResponseEntity.ok("Scores saved and calculated");
    }

    // --- User Management ---

    @GetMapping("/users")
    public ResponseEntity<List<MemberDto>> listUsers() {
        List<Member> members = memberRepository.findAll();
        List<MemberDto> dtos = members.stream()
                .map(MemberDto::new)
                .collect(Collectors.toList());
        return ResponseEntity.ok(dtos);
    }

    @PostMapping("/users")
    public ResponseEntity<String> createUser(@RequestBody @Valid UserCreateRequest request, BindingResult bindingResult) {
        if (bindingResult.hasErrors()) {
            String errorMessage = bindingResult.getAllErrors().stream()
                    .map(error -> error.getDefaultMessage())
                    .collect(Collectors.joining(", "));
            return ResponseEntity.badRequest().body(errorMessage);
        }

        if (memberRepository.existsById(request.getPlayerId())) {
            return ResponseEntity.badRequest().body("Player ID already exists");
        }
        if (memberRepository.findByUserId(request.getUserId()).isPresent()) {
            return ResponseEntity.badRequest().body("User ID already exists");
        }

        Member member = Member.builder()
                .playerId(request.getPlayerId())
                .userId(request.getUserId())
                .password(passwordEncoder.encode(request.getPassword()))
                .name(request.getName())
                .role(Member.Role.USER) // Default
                .build();

        memberRepository.save(member);
        return ResponseEntity.ok("User created");
    }

    @PutMapping("/users/{playerId}")
    public ResponseEntity<String> updateUser(@PathVariable(name = "playerId") Long playerId,
                                             @RequestBody UserUpdateRequest request) {
        Member member = memberRepository.findById(playerId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        if (request.getUserId() != null && !request.getUserId().isEmpty()) {
            if (!request.getUserId().equals(member.getUserId())) {
                if (memberRepository.findByUserId(request.getUserId()).isPresent()) {
                    return ResponseEntity.badRequest().body("User ID already exists");
                }
                member.setUserId(request.getUserId());
            }
        }

        if (request.getName() != null && !request.getName().isEmpty()) {
            member.setName(request.getName());
        }

        if (request.getPassword() != null && !request.getPassword().isEmpty()) {
            member.setPassword(passwordEncoder.encode(request.getPassword()));
        }

        memberRepository.save(member);
        return ResponseEntity.ok("User updated");
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
        private Integer rounds;
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

    @Data
    public static class UserCreateRequest {
        @NotNull(message = "Player ID is required")
        private Long playerId;

        @NotBlank(message = "User ID is required")
        private String userId;

        @NotBlank(message = "Password is required")
        @ToString.Exclude
        private String password;

        @NotBlank(message = "Name is required")
        private String name;
    }

    @Data
    public static class UserUpdateRequest {
        private String userId;
        private String password; // Optional, only if changing
        private String name;
    }

    @Data
    public static class MemberDto {
        private Long playerId;
        private String userId;
        private String name;
        private Member.Role role;

        public MemberDto(Member member) {
            this.playerId = member.getPlayerId();
            this.userId = member.getUserId();
            this.name = member.getName();
            this.role = member.getRole();
        }
    }
}
