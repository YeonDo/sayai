package com.sayai.record.fantasy.service;

import com.sayai.record.fantasy.dto.DraftEventDto;
import com.sayai.record.fantasy.dto.DraftRequest;
import com.sayai.record.fantasy.dto.FantasyPlayerDto;
import com.sayai.record.fantasy.dto.RosterUpdateDto;
import com.sayai.record.fantasy.entity.DraftPick;
import com.sayai.record.fantasy.entity.FantasyGame;
import com.sayai.record.fantasy.entity.FantasyParticipant;
import com.sayai.record.fantasy.entity.FantasyPlayer;
import com.sayai.record.fantasy.repository.DraftPickRepository;
import com.sayai.record.fantasy.repository.FantasyGameRepository;
import com.sayai.record.fantasy.repository.FantasyParticipantRepository;
import com.sayai.record.fantasy.repository.FantasyPlayerRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class FantasyDraftService {

    private final FantasyPlayerRepository fantasyPlayerRepository;
    private final DraftPickRepository draftPickRepository;
    private final FantasyGameRepository fantasyGameRepository;
    private final FantasyParticipantRepository fantasyParticipantRepository;
    private final DraftValidator draftValidator;
    private final SimpMessagingTemplate messagingTemplate;
    private final ObjectProvider<DraftScheduler> draftSchedulerProvider;

    @Transactional
    public void joinGame(Long gameSeq, Long playerId, String preferredTeam, String teamName) {
        FantasyGame game = fantasyGameRepository.findById(gameSeq)
                .orElseThrow(() -> new IllegalArgumentException("Invalid Game Seq"));

        if (game.getStatus() != FantasyGame.GameStatus.WAITING) {
            throw new IllegalStateException("Cannot join game. Status is " + game.getStatus());
        }

        // Check if already joined
        if (fantasyParticipantRepository.findByFantasyGameSeqAndPlayerId(gameSeq, playerId).isPresent()) {
            throw new IllegalStateException("Player already joined this game");
        }

        FantasyParticipant participant = FantasyParticipant.builder()
                .fantasyGameSeq(gameSeq)
                .playerId(playerId)
                .preferredTeam(preferredTeam)
                .teamName(teamName)
                .build();

        fantasyParticipantRepository.save(participant);
    }

    @Transactional(readOnly = true)
    public List<FantasyPlayerDto> getAvailablePlayers(Long gameSeq, String team, String position, String search, String sort) {
        // 1. Get all picks for this game
        List<DraftPick> picks = draftPickRepository.findByFantasyGameSeq(gameSeq);
        Set<Long> pickedPlayerSeqs = picks.stream()
                .map(DraftPick::getFantasyPlayerSeq)
                .collect(Collectors.toSet());

        // 2. Get filtered players from DB
        List<FantasyPlayer> filteredPlayers = fantasyPlayerRepository.findPlayers(team, position, search);

        // Sort
        if (sort != null) {
            if ("cost_desc".equals(sort)) {
                filteredPlayers.sort((p1, p2) -> {
                    int c1 = p1.getCost() == null ? 0 : p1.getCost();
                    int c2 = p2.getCost() == null ? 0 : p2.getCost();
                    return Integer.compare(c2, c1);
                });
            } else if ("cost_asc".equals(sort)) {
                filteredPlayers.sort((p1, p2) -> {
                    int c1 = p1.getCost() == null ? 0 : p1.getCost();
                    int c2 = p2.getCost() == null ? 0 : p2.getCost();
                    return Integer.compare(c1, c2);
                });
            }
        }

        return filteredPlayers.stream()
                .filter(p -> !pickedPlayerSeqs.contains(p.getSeq()))
                .map(FantasyPlayerDto::from)
                .collect(Collectors.toList());
    }

    @Transactional
    public void draftPlayer(DraftRequest request) {
        // 1. Check Game Status
        FantasyGame game = fantasyGameRepository.findById(request.getFantasyGameSeq())
                .orElseThrow(() -> new IllegalArgumentException("Invalid Game Seq"));

        if (game.getStatus() != FantasyGame.GameStatus.DRAFTING) {
            throw new IllegalStateException("Game is not in DRAFTING status");
        }

        // Check Turn
        NextPickInfo nextPick = getNextPickInfo(game);
        if (!nextPick.pickerId.equals(request.getPlayerId())) {
            throw new IllegalStateException("It is not your turn. Current turn: " + nextPick.pickerId);
        }

        // 2. Check availability
        boolean isPicked = draftPickRepository.existsByFantasyGameSeqAndFantasyPlayerSeq(
                request.getFantasyGameSeq(),
                request.getFantasyPlayerSeq()
        );
        if (isPicked) {
            throw new IllegalStateException("Player already picked");
        }

        // 3. Validate Rules
        FantasyPlayer targetPlayer = fantasyPlayerRepository.findById(request.getFantasyPlayerSeq())
                .orElseThrow(() -> new IllegalArgumentException("Invalid Player Seq"));

        // Get Current Picks for this user
        List<DraftPick> userPicks = draftPickRepository.findByFantasyGameSeqAndPlayerId(
                request.getFantasyGameSeq(), request.getPlayerId());

        Set<Long> pickedSeqs = userPicks.stream()
                .map(DraftPick::getFantasyPlayerSeq)
                .collect(Collectors.toSet());
        List<FantasyPlayer> currentTeam = fantasyPlayerRepository.findAllById(pickedSeqs);

        // Salary Cap Check
        if (game.getSalaryCap() != null && game.getSalaryCap() > 0) {
            int currentCost = currentTeam.stream().mapToInt(p -> p.getCost() == null ? 0 : p.getCost()).sum();
            int newPlayerCost = targetPlayer.getCost() == null ? 0 : targetPlayer.getCost();
            if (currentCost + newPlayerCost > game.getSalaryCap()) {
                throw new IllegalStateException("Salary Cap Exceeded: " + (currentCost + newPlayerCost) + " / " + game.getSalaryCap());
            }
        }

        // Get Participant Info (needed for Rule 2)
        FantasyParticipant participant = fantasyParticipantRepository.findByFantasyGameSeqAndPlayerId(
                request.getFantasyGameSeq(), request.getPlayerId())
                .orElse(null); // Might be null if user didn't join explicitly (Rule 1 usually allows ad-hoc?)
                                // Actually better to require join for consistent logic, but let's handle null gracefully for Rule 1.

        draftValidator.validate(game, targetPlayer, currentTeam, participant);

        // 4. Save Pick
        // Auto-Assign Position Logic
        String assignedPos = determineInitialPosition(userPicks, targetPlayer);

        // Calculate pick number
        long count = draftPickRepository.countByFantasyGameSeq(request.getFantasyGameSeq());
        int pickNumber = (int) count + 1;

        DraftPick pick = DraftPick.builder()
                .fantasyGameSeq(request.getFantasyGameSeq())
                .playerId(request.getPlayerId())
                .fantasyPlayerSeq(request.getFantasyPlayerSeq())
                .pickNumber(pickNumber)
                .assignedPosition(assignedPos)
                .build();

        draftPickRepository.save(pick);

        // Update Deadline for NEXT pick
        if (game.getDraftTimeLimit() != null && game.getDraftTimeLimit() > 0) {
            game.setNextPickDeadline(LocalDateTime.now().plusMinutes(game.getDraftTimeLimit()));
            // No need to save game explicitly if transaction handles dirty check, but safest to save or rely on transactional
        }

        // Get NEXT Pick Info
        NextPickInfo nextNext = getNextPickInfo(game);

        // Check for Draft Completion
        long totalPicks = draftPickRepository.countByFantasyGameSeq(request.getFantasyGameSeq());
        List<FantasyParticipant> participants = fantasyParticipantRepository.findByFantasyGameSeq(request.getFantasyGameSeq());

        int totalPlayersPerParticipant = draftValidator.getTotalPlayerCount(game.getRuleType());

        boolean isFinished = false;
        if (!participants.isEmpty() && totalPicks >= participants.size() * (long)totalPlayersPerParticipant) {
            game.setStatus(FantasyGame.GameStatus.ONGOING);
            game.setNextPickDeadline(null);
            fantasyGameRepository.save(game); // Ensure status persist
            isFinished = true;
            if (TransactionSynchronizationManager.isActualTransactionActive()) {
                TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                    @Override
                    public void afterCommit() {
                        draftSchedulerProvider.getObject().removeActiveGame(request.getFantasyGameSeq());
                    }
                });
            } else {
                draftSchedulerProvider.getObject().removeActiveGame(request.getFantasyGameSeq());
            }
        }

        // Broadcast Event
        DraftEventDto event = DraftEventDto.builder()
                .type(isFinished ? "FINISH" : "PICK")
                .fantasyGameSeq(request.getFantasyGameSeq())
                .playerId(request.getPlayerId())
                .fantasyPlayerSeq(request.getFantasyPlayerSeq())
                .playerName(targetPlayer.getName())
                .playerTeam(targetPlayer.getTeam())
                .pickNumber(pickNumber)
                .message(isFinished ? "Draft Completed!" : "Player " + request.getPlayerId() + " picked " + targetPlayer.getName() + " (Pick #" + pickNumber + ")")
                .nextPickerId(isFinished ? null : nextNext.pickerId)
                .nextPickDeadline(game.getNextPickDeadline() != null ? game.getNextPickDeadline().atZone(ZoneId.of("UTC")) : null)
                .round(isFinished ? null : nextNext.round)
                .pickInRound(isFinished ? null : nextNext.pickInRound)
                .build();

        messagingTemplate.convertAndSend("/topic/draft/" + request.getFantasyGameSeq(), event);
    }

    public static class NextPickInfo {
        public Long pickerId;
        public int round;
        public int pickInRound;
    }

    public NextPickInfo getNextPickInfo(FantasyGame game) {
        long totalPicks = draftPickRepository.countByFantasyGameSeq(game.getSeq());
        List<FantasyParticipant> participants = fantasyParticipantRepository.findByFantasyGameSeq(game.getSeq());
        participants.sort(Comparator.comparingInt(FantasyParticipant::getDraftOrder));
        int n = participants.size();
        if (n == 0) throw new IllegalStateException("No participants");

        int round = (int) (totalPicks / n) + 1;
        int index = (int) (totalPicks % n); // 0 to n-1

        int draftOrderIndex; // 1-based draftOrder
        if (round % 2 != 0) { // Odd
            draftOrderIndex = index + 1;
        } else { // Even (Snake)
            draftOrderIndex = n - index;
        }

        // Find participant with this draftOrder
        // Since list is sorted by draftOrder, index is draftOrderIndex - 1
        FantasyParticipant nextPicker = participants.get(draftOrderIndex - 1);

        NextPickInfo info = new NextPickInfo();
        info.pickerId = nextPicker.getPlayerId();
        info.round = round;
        info.pickInRound = index + 1;
        return info;
    }

    @Transactional(readOnly = true)
    public List<FantasyPlayerDto> getPickedPlayers(Long gameSeq, Long playerId) {
        // Fetch picks
        List<DraftPick> picks = draftPickRepository.findByFantasyGameSeqAndPlayerId(gameSeq, playerId);

        // Map Player Seq to Pick Number for sorting
        Map<Long, Integer> pickOrderMap = picks.stream()
                .collect(Collectors.toMap(DraftPick::getFantasyPlayerSeq, DraftPick::getPickNumber));

        Set<Long> pickedSeqs = pickOrderMap.keySet();
        List<FantasyPlayer> players = fantasyPlayerRepository.findAllById(pickedSeqs);

        return players.stream()
                .map(FantasyPlayerDto::from)
                .sorted(Comparator.comparingInt(p -> pickOrderMap.getOrDefault(p.getSeq(), 0)))
                .collect(Collectors.toList());
    }

    private String determineInitialPosition(List<DraftPick> existingPicks, FantasyPlayer newPlayer) {
        Set<String> occupied = existingPicks.stream()
                .map(DraftPick::getAssignedPosition)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        String[] positions = newPlayer.getPosition().split(",");
        String primaryPos = positions[0].trim();

        if (isPitcher(primaryPos)) {
            // Pitcher logic: find first open slot (SP-1..SP-4, RP-1..RP-4, CL-1)
            // Or simple logic: Just store "SP", "RP", "CL". Frontend handles slots.
            // But if user has 4 SPs, 5th SP -> Bench.
            long spCount = occupied.stream().filter(p -> p.equals("SP")).count();
            long rpCount = occupied.stream().filter(p -> p.equals("RP")).count();
            long clCount = occupied.stream().filter(p -> p.equals("CL") || p.equals("CP")).count();

            if (primaryPos.equals("SP")) return spCount < 4 ? "SP" : null;
            if (primaryPos.equals("RP")) return rpCount < 4 ? "RP" : null;
            if (primaryPos.equals("CL") || primaryPos.equals("CP")) return clCount < 1 ? "CL" : null;
            return null; // Bench
        } else {
            // Batter Logic
            if (!occupied.contains(primaryPos)) {
                return primaryPos;
            }
            // Try DH
            if (!occupied.contains("DH")) {
                return "DH";
            }
            // Bench
            return null;
        }
    }

    private boolean isPitcher(String pos) {
        return pos.equals("SP") || pos.equals("RP") || pos.equals("CL") || pos.equals("CP");
    }

    @Transactional
    public void updateRoster(Long gameSeq, Long playerId, RosterUpdateDto updateDto) {
        List<DraftPick> myPicks = draftPickRepository.findByFantasyGameSeqAndPlayerId(gameSeq, playerId);
        Map<Long, DraftPick> pickMap = myPicks.stream()
                .collect(Collectors.toMap(DraftPick::getFantasyPlayerSeq, Function.identity()));

        if (updateDto.getEntries() != null) {
            for (RosterUpdateDto.RosterEntry entry : updateDto.getEntries()) {
                DraftPick pick = pickMap.get(entry.getFantasyPlayerSeq());
                if (pick != null) {
                    pick.setAssignedPosition(entry.getAssignedPosition());
                }
            }
        }
        draftPickRepository.saveAll(myPicks);
    }

    @Transactional
    public void autoPick(Long gameSeq) {
        FantasyGame game = fantasyGameRepository.findById(gameSeq)
                .orElseThrow(() -> new IllegalArgumentException("Invalid Game Seq"));

        if (game.getStatus() != FantasyGame.GameStatus.DRAFTING) {
            return;
        }

        // Validate deadline again to avoid race conditions
        if (game.getNextPickDeadline() != null && game.getNextPickDeadline().isAfter(LocalDateTime.now())) {
            return;
        }

        NextPickInfo nextPick = getNextPickInfo(game);
        Long playerId = nextPick.pickerId;

        // Fetch needed data
        List<DraftPick> picks = draftPickRepository.findByFantasyGameSeq(gameSeq);
        Set<Long> pickedPlayerSeqs = picks.stream()
                .map(DraftPick::getFantasyPlayerSeq)
                .collect(Collectors.toSet());

        List<FantasyPlayer> allPlayers = fantasyPlayerRepository.findAll();
        List<FantasyPlayer> available = allPlayers.stream()
                .filter(p -> !pickedPlayerSeqs.contains(p.getSeq()))
                .collect(Collectors.toList());

        FantasyParticipant participant = fantasyParticipantRepository.findByFantasyGameSeqAndPlayerId(gameSeq, playerId).orElseThrow();
        List<FantasyPlayer> candidates = available;

        // Rule 2 Logic for Auto Pick
        if (game.getRuleType() == FantasyGame.RuleType.RULE_2 && nextPick.round == 1) {
             String pref = participant.getPreferredTeam();
             if (pref != null) {
                 String prefLower = pref.trim().toLowerCase();
                 candidates = available.stream().filter(p ->
                     p.getTeam().toLowerCase().contains(prefLower) ||
                     prefLower.contains(p.getTeam().toLowerCase())
                 ).collect(Collectors.toList());
             }
        }

        Collections.shuffle(candidates);

        // Prepare current team for validation
        List<DraftPick> userPicks = picks.stream().filter(p -> p.getPlayerId().equals(playerId)).collect(Collectors.toList());
        Set<Long> userPickedSeqs = userPicks.stream().map(DraftPick::getFantasyPlayerSeq).collect(Collectors.toSet());
        // Optimize: we have allPlayers, can find from there
        List<FantasyPlayer> currentTeam = allPlayers.stream().filter(p -> userPickedSeqs.contains(p.getSeq())).collect(Collectors.toList());

        FantasyPlayer selected = null;
        for (FantasyPlayer p : candidates) {
            try {
                draftValidator.validate(game, p, currentTeam, participant);
                selected = p;
                break;
            } catch (Exception e) {
                // Invalid, try next
            }
        }

        if (selected != null) {
            DraftRequest req = new DraftRequest();
            req.setFantasyGameSeq(gameSeq);
            req.setFantasyPlayerSeq(selected.getSeq());
            req.setPlayerId(playerId);
            draftPlayer(req);
        } else {
            // Log or handle no valid pick found
            log.error("AutoPick failed: No valid player found for game {} user {}", gameSeq, playerId);
        }
    }
}
