package com.sayai.record.fantasy.service;

import com.sayai.record.fantasy.dto.*;
import com.sayai.record.fantasy.entity.DraftPick;
import com.sayai.record.fantasy.entity.FantasyGame;
import com.sayai.record.fantasy.entity.FantasyParticipant;
import com.sayai.record.fantasy.entity.FantasyPlayer;
import com.sayai.record.fantasy.repository.DraftPickRepository;
import com.sayai.record.fantasy.repository.FantasyGameRepository;
import com.sayai.record.fantasy.repository.FantasyParticipantRepository;
import com.sayai.record.fantasy.repository.FantasyPlayerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class FantasyGameService {

    private final FantasyGameRepository fantasyGameRepository;
    private final FantasyParticipantRepository fantasyParticipantRepository;
    private final DraftPickRepository draftPickRepository;
    private final FantasyPlayerRepository fantasyPlayerRepository;
    private final SimpMessagingTemplate messagingTemplate;
    private final FantasyDraftService fantasyDraftService;
    private final DraftScheduler draftScheduler;

    @Transactional(readOnly = true)
    public List<FantasyGameDto> getDashboardGames(Long userId) {
        // 1. Get all active games (not FINISHED)
        List<FantasyGame> games = fantasyGameRepository.findAll().stream()
                .filter(g -> g.getStatus() != FantasyGame.GameStatus.FINISHED)
                .collect(Collectors.toList());

        // 2. Get all participants for these games to calculate counts and check join status
        List<Long> gameSeqs = games.stream().map(FantasyGame::getSeq).collect(Collectors.toList());
        List<FantasyParticipant> participants = fantasyParticipantRepository.findAll().stream()
                .filter(p -> gameSeqs.contains(p.getFantasyGameSeq()))
                .collect(Collectors.toList());

        Map<Long, Long> countMap = participants.stream()
                .collect(Collectors.groupingBy(FantasyParticipant::getFantasyGameSeq, Collectors.counting()));

        Map<Long, FantasyParticipant> userParticipation = participants.stream()
                .filter(p -> p.getPlayerId().equals(userId))
                .collect(Collectors.toMap(FantasyParticipant::getFantasyGameSeq, Function.identity(), (existing, replacement) -> existing));

        return games.stream().map(game -> {
            FantasyGameDto dto = FantasyGameDto.from(game);
            dto.setParticipantCount(countMap.getOrDefault(game.getSeq(), 0L).intValue());
            if (userParticipation.containsKey(game.getSeq())) {
                dto.setJoined(true);
                dto.setMyTeamName(userParticipation.get(game.getSeq()).getTeamName());
            } else {
                dto.setJoined(false);
            }
            return dto;
        }).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<FantasyGameDto> getMyGames(Long userId) {
        // Find games where user participated
        List<FantasyParticipant> myParticipations = fantasyParticipantRepository.findAll().stream()
                .filter(p -> p.getPlayerId().equals(userId))
                .collect(Collectors.toList());

        List<Long> gameSeqs = myParticipations.stream()
                .map(FantasyParticipant::getFantasyGameSeq)
                .collect(Collectors.toList());

        List<FantasyGame> games = fantasyGameRepository.findAllById(gameSeqs);

        return games.stream().map(FantasyGameDto::from).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public Long findDraftingGameId(Long userId) {
        List<FantasyParticipant> myParticipations = fantasyParticipantRepository.findAll().stream()
                .filter(p -> p.getPlayerId().equals(userId))
                .collect(Collectors.toList());

        List<Long> gameSeqs = myParticipations.stream()
                .map(FantasyParticipant::getFantasyGameSeq)
                .collect(Collectors.toList());

        return fantasyGameRepository.findAllById(gameSeqs).stream()
                .filter(g -> g.getStatus() == FantasyGame.GameStatus.DRAFTING)
                .map(FantasyGame::getSeq)
                .findFirst()
                .orElse(null);
    }

    @Transactional
    public FantasyGame createGame(String title, FantasyGame.RuleType ruleType, FantasyGame.ScoringType scoringType,
                                  String scoringSettings, Integer maxParticipants, java.time.LocalDateTime draftDate, String gameDuration, Integer draftTimeLimit,
                                  Boolean useFirstPickRule, Integer salaryCap, Boolean useTeamRestriction, Integer rounds) {
        FantasyGame game = FantasyGame.builder()
                .title(title)
                .ruleType(ruleType)
                .scoringType(scoringType)
                .scoringSettings(scoringSettings)
                .maxParticipants(maxParticipants)
                .draftDate(draftDate)
                .gameDuration(gameDuration)
                .draftTimeLimit(draftTimeLimit != null ? draftTimeLimit : 10)
                .useFirstPickRule(useFirstPickRule)
                .salaryCap(salaryCap)
                .useTeamRestriction(useTeamRestriction)
                .rounds(rounds)
                .status(FantasyGame.GameStatus.WAITING)
                .build();
        return fantasyGameRepository.save(game);
    }

    @Transactional
    public void updateGameStatus(Long gameSeq, FantasyGame.GameStatus status) {
        FantasyGame game = fantasyGameRepository.findById(gameSeq)
                .orElseThrow(() -> new IllegalArgumentException("Game not found"));
        game.setStatus(status);

        if (status == FantasyGame.GameStatus.DRAFTING) {
            updateSchedulerSafely(gameSeq, true);
        } else {
            updateSchedulerSafely(gameSeq, false);
        }
    }

    @Transactional
    public void startGame(Long gameSeq) {
        FantasyGame game = fantasyGameRepository.findById(gameSeq)
                .orElseThrow(() -> new IllegalArgumentException("Game not found"));

        if (game.getStatus() != FantasyGame.GameStatus.WAITING) {
            throw new IllegalStateException("Game must be in WAITING status to start.");
        }

        List<FantasyParticipant> participants = fantasyParticipantRepository.findByFantasyGameSeq(gameSeq);
        if (participants.isEmpty()) {
            throw new IllegalStateException("Cannot start game with no participants.");
        }

        // Shuffle and assign order
        Collections.shuffle(participants);

        int order = 1;
        for (FantasyParticipant p : participants) {
            p.setDraftOrder(order++);
        }
        fantasyParticipantRepository.saveAll(participants);

        // Update Game Status
        game.setStatus(FantasyGame.GameStatus.DRAFTING);
        updateSchedulerSafely(gameSeq, true);

        // Set Initial Deadline
        if (game.getDraftTimeLimit() != null && game.getDraftTimeLimit() > 0) {
            game.setNextPickDeadline(LocalDateTime.now().plusMinutes(game.getDraftTimeLimit()));
        }

        // Prepare Event Data
        List<ParticipantRosterDto> orderList = participants.stream()
            .sorted(Comparator.comparingInt(FantasyParticipant::getDraftOrder))
            .map(p -> ParticipantRosterDto.builder()
                .participantId(p.getPlayerId())
                .teamName(p.getTeamName())
                .preferredTeam(p.getPreferredTeam())
                .draftOrder(p.getDraftOrder())
                .build())
            .collect(Collectors.toList());

        DraftEventDto event = DraftEventDto.builder()
                .type("START")
                .fantasyGameSeq(gameSeq)
                .message("Draft Started")
                .draftOrder(orderList)
                .nextPickerId(orderList.get(0).getParticipantId()) // First picker
                .nextPickDeadline(game.getNextPickDeadline() != null ? game.getNextPickDeadline().atZone(ZoneId.of("UTC")) : null)
                .round(1)
                .pickInRound(1)
                .build();

        messagingTemplate.convertAndSend("/topic/draft/" + gameSeq, event);
    }

    @Transactional(readOnly = true)
    public List<DraftLogDto> getDraftPicks(Long gameSeq) {
        List<DraftPick> picks = draftPickRepository.findByFantasyGameSeq(gameSeq);

        Set<Long> playerSeqs = picks.stream().map(DraftPick::getFantasyPlayerSeq).collect(Collectors.toSet());
        Map<Long, FantasyPlayer> players = fantasyPlayerRepository.findAllById(playerSeqs).stream()
                .collect(Collectors.toMap(FantasyPlayer::getSeq, Function.identity()));

        List<FantasyParticipant> participants = fantasyParticipantRepository.findByFantasyGameSeq(gameSeq);
        Map<Long, String> teamNames = participants.stream()
                .collect(Collectors.toMap(FantasyParticipant::getPlayerId, FantasyParticipant::getTeamName, (a, b) -> a));

        return picks.stream().map(pick -> {
            FantasyPlayer player = players.get(pick.getFantasyPlayerSeq());
            String teamName = teamNames.getOrDefault(pick.getPlayerId(), "Unknown");
            return DraftLogDto.builder()
                    .pickNumber(pick.getPickNumber())
                    .playerName(player != null ? player.getName() : "Unknown")
                    .playerTeam(player != null ? player.getTeam() : "")
                    .playerPosition(player != null ? player.getPosition() : "")
                    .pickedByTeamName(teamName)
                    .pickedAt(pick.getPickedAt())
                    .build();
        }).sorted(Comparator.comparing(DraftLogDto::getPickNumber)).collect(Collectors.toList());
    }

    private void updateSchedulerSafely(Long gameSeq, boolean add) {
        Runnable action = add ? () -> draftScheduler.addActiveGame(gameSeq) : () -> draftScheduler.removeActiveGame(gameSeq);
        if (TransactionSynchronizationManager.isActualTransactionActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    action.run();
                }
            });
        } else {
            action.run();
        }
    }

    @Transactional(readOnly = true)
    public String exportRoster(Long gameSeq) {
        FantasyGame game = fantasyGameRepository.findById(gameSeq)
                .orElseThrow(() -> new IllegalArgumentException("Game not found"));

        // Allow export for ONGOING and FINISHED
        if (game.getStatus() != FantasyGame.GameStatus.ONGOING && game.getStatus() != FantasyGame.GameStatus.FINISHED) {
            // throw new IllegalStateException("Game must be ONGOING or FINISHED to export.");
            // Or just return empty or proceed. Requirement says "ONGOING".
        }

        List<FantasyParticipant> participants = fantasyParticipantRepository.findByFantasyGameSeq(gameSeq);
        List<DraftPick> allPicks = draftPickRepository.findByFantasyGameSeq(gameSeq);

        Set<Long> playerSeqs = allPicks.stream().map(DraftPick::getFantasyPlayerSeq).collect(Collectors.toSet());
        Map<Long, FantasyPlayer> players = fantasyPlayerRepository.findAllById(playerSeqs).stream()
                .collect(Collectors.toMap(FantasyPlayer::getSeq, Function.identity()));

        Map<Long, List<DraftPick>> picksByPart = allPicks.stream().collect(Collectors.groupingBy(DraftPick::getPlayerId));

        StringBuilder sb = new StringBuilder();
        // Header (Optional, but good for context)
        // sb.append("Participant\tBatters(1-9)\tPitchers(1-9)\n");

        for (FantasyParticipant p : participants) {
            sb.append(p.getTeamName());

            List<DraftPick> picks = picksByPart.getOrDefault(p.getPlayerId(), Collections.emptyList());
            List<FantasyPlayer> roster = picks.stream()
                    .map(pick -> players.get(pick.getFantasyPlayerSeq()))
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());

            List<String> batters = new ArrayList<>();
            List<String> pitchers = new ArrayList<>();

            for (FantasyPlayer fp : roster) {
                // Determine logic from position string or assigned position (but draftPick assignedPosition might be null if bench)
                // Use simple heuristic: if position contains P, it's pitcher?
                // Or check explicit types. KBO data usually "SP", "RP", "P" etc.
                // Rule1Validator uses specific codes.
                String pos = fp.getPosition();
                // Better to rely on assignedPosition if available, but for export we might just use player's primary pos.
                // Let's use simple logic: if (SP, RP, CP, CL, P) -> Pitcher.

                boolean isPitcher = pos.contains("SP") || pos.contains("RP") || pos.contains("CP") || pos.contains("CL") || pos.equals("P");
                if (isPitcher) {
                    pitchers.add(fp.getName());
                } else {
                    batters.add(fp.getName());
                }
            }

            // Fill slots (9 batters, 9 pitchers as per request template hint)
            for (int i = 0; i < 9; i++) {
                sb.append("\t");
                if (i < batters.size()) sb.append(batters.get(i));
            }
            for (int i = 0; i < 9; i++) {
                sb.append("\t");
                if (i < pitchers.size()) sb.append(pitchers.get(i));
            }
            sb.append("\n");
        }
        return sb.toString();
    }

    @Transactional(readOnly = true)
    public FantasyGameDetailDto getGameDetails(Long gameSeq) {
        FantasyGame game = fantasyGameRepository.findById(gameSeq)
                .orElseThrow(() -> new IllegalArgumentException("Game not found"));

        List<FantasyParticipant> participants = fantasyParticipantRepository.findByFantasyGameSeq(gameSeq);

        // Fetch all picks
        List<DraftPick> allPicks = draftPickRepository.findByFantasyGameSeq(gameSeq);
        Map<Long, List<DraftPick>> picksByParticipant = allPicks.stream()
                .collect(Collectors.groupingBy(DraftPick::getPlayerId));

        // Fetch all relevant fantasy players
        Set<Long> fantasyPlayerSeqs = allPicks.stream()
                .map(DraftPick::getFantasyPlayerSeq)
                .collect(Collectors.toSet());
        Map<Long, FantasyPlayer> fantasyPlayers = fantasyPlayerRepository.findAllById(fantasyPlayerSeqs).stream()
                .collect(Collectors.toMap(FantasyPlayer::getSeq, Function.identity()));

        // Calculate Next Picker (if Drafting)
        Long nextPickerId = null;
        Integer round = null;
        Integer pickInRound = null;
        if (game.getStatus() == FantasyGame.GameStatus.DRAFTING) {
             try {
                 FantasyDraftService.NextPickInfo info = fantasyDraftService.getNextPickInfo(game);
                 nextPickerId = info.pickerId;
                 round = info.round;
                 pickInRound = info.pickInRound;
             } catch (Exception e) {
                 // Ignore if calculation fails (e.g. no participants yet)
             }
        }

        List<ParticipantRosterDto> rosterDtos = participants.stream().map(p -> {
            List<DraftPick> myPicks = picksByParticipant.getOrDefault(p.getPlayerId(), Collections.emptyList());
            List<FantasyPlayerDto> roster = myPicks.stream()
                    .map(pick -> {
                        FantasyPlayer fp = fantasyPlayers.get(pick.getFantasyPlayerSeq());
                        if (fp == null) return null;
                        FantasyPlayerDto dto = FantasyPlayerDto.from(fp);
                        dto.setAssignedPosition(pick.getAssignedPosition());
                        return dto;
                    })
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());

            return ParticipantRosterDto.builder()
                    .participantId(p.getPlayerId())
                    .teamName(p.getTeamName())
                    .preferredTeam(p.getPreferredTeam())
                    .draftOrder(p.getDraftOrder())
                    .roster(roster)
                    .build();
        }).collect(Collectors.toList());

        return FantasyGameDetailDto.builder()
                .seq(game.getSeq())
                .title(game.getTitle())
                .ruleType(game.getRuleType().name())
                .scoringType(game.getScoringType().name())
                .scoringSettings(game.getScoringSettings())
                .status(game.getStatus().name())
                .gameDuration(game.getGameDuration())
                .draftTimeLimit(game.getDraftTimeLimit())
                .useFirstPickRule(game.getUseFirstPickRule())
                .salaryCap(game.getSalaryCap())
                .useTeamRestriction(game.getUseTeamRestriction())
                .rounds(game.getRounds())
                .participantCount(participants.size())
                .maxParticipants(game.getMaxParticipants())
                .nextPickerId(nextPickerId)
                .nextPickDeadline(game.getNextPickDeadline() != null ? game.getNextPickDeadline().atZone(ZoneId.of("UTC")) : null)
                .roundNum(round)
                .pickInRound(pickInRound)
                .participants(rosterDtos)
                .build();
    }
}
