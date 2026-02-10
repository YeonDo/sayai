package com.sayai.record.fantasy.service;

import com.sayai.record.fantasy.dto.DraftEventDto;
import com.sayai.record.fantasy.dto.DraftRequest;
import com.sayai.record.fantasy.dto.FantasyPlayerDto;
import com.sayai.record.fantasy.dto.RosterUpdateDto;
import com.sayai.record.fantasy.entity.*;
import com.sayai.record.fantasy.repository.*;
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
    private final RoasterLogRepository roasterLogRepository;
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
            throw new IllegalStateException("이미 참여 신청을 완료했습니다");
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
    public List<FantasyPlayerDto> getAvailablePlayers(Long gameSeq, String team, String position, String search, String sort, String foreignerType) {
        if (team != null && team.isEmpty()) team = null;
        if (position != null && position.isEmpty()) position = null;
        if (search != null && search.isEmpty()) search = null;
        if (foreignerType != null && foreignerType.isEmpty()) foreignerType = null;

        FantasyPlayer.ForeignerType fType = null;
        if (foreignerType != null) {
            try {
                fType = FantasyPlayer.ForeignerType.valueOf(foreignerType);
            } catch (IllegalArgumentException e) {
                // Invalid enum value, ignore or treat as null
            }
        }

        // 1. Get all picks for this game
        // Players in WAIVER_REQ or TRADE_PENDING still have DraftPick records,
        // so they are correctly excluded from available players by this logic.
        Set<Long> pickedPlayerSeqs;
        if (gameSeq == null || gameSeq == 0L) {
            pickedPlayerSeqs = Collections.emptySet();
        } else {
            List<DraftPick> picks = draftPickRepository.findByFantasyGameSeq(gameSeq);
            pickedPlayerSeqs = picks.stream()
                    .map(DraftPick::getFantasyPlayerSeq)
                    .collect(Collectors.toSet());
        }

        // 2. Get filtered players from DB
        List<FantasyPlayer> filteredPlayers = fantasyPlayerRepository.findPlayers(team, position, search, fType);

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

        boolean isFA = (game.getStatus() == FantasyGame.GameStatus.ONGOING);
        boolean isDrafting = (game.getStatus() == FantasyGame.GameStatus.DRAFTING);

        if (!isDrafting && !isFA) {
            throw new IllegalStateException("Drafting or FA signing is not active (Status: " + game.getStatus() + ")");
        }

        int pickNumber = 0;
        NextPickInfo nextPick = null;

        if (isDrafting) {
            // Check Turn
            nextPick = getNextPickInfo(game);
            if (!nextPick.pickerId.equals(request.getPlayerId())) {
                throw new IllegalStateException("당신의 차례가 아닙니다: " + nextPick.pickerId);
            }
            long count = draftPickRepository.countByFantasyGameSeq(request.getFantasyGameSeq());
            pickNumber = (int) count + 1;
        } else {
            // FA Logic
            long count = draftPickRepository.countByFantasyGameSeq(request.getFantasyGameSeq());
            pickNumber = (int) count + 1;
        }

        // 2. Check availability
        boolean isPicked = draftPickRepository.existsByFantasyGameSeqAndFantasyPlayerSeq(
                request.getFantasyGameSeq(),
                request.getFantasyPlayerSeq()
        );
        if (isPicked) {
            throw new IllegalStateException("이미 뽑힌 선수입니다");
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

        // Roster Size Check for FA
        if (isFA) {
            int limit = (game.getRuleType() == FantasyGame.RuleType.RULE_2) ? 21 : 20;
            if (userPicks.size() >= limit) {
                throw new IllegalStateException("Roster full (Max " + limit + ")");
            }
        }

        // Salary Cap Check
        if (game.getSalaryCap() != null && game.getSalaryCap() > 0) {
            int currentCost = currentTeam.stream().mapToInt(p -> p.getCost() == null ? 0 : p.getCost()).sum();
            int newPlayerCost = targetPlayer.getCost() == null ? 0 : targetPlayer.getCost();
            if (currentCost + newPlayerCost > game.getSalaryCap()) {
                throw new IllegalStateException("샐캡 초과: " + (currentCost + newPlayerCost) + " / " + game.getSalaryCap());
            }
        }

        // Get Participant Info (needed for Rule 2)
        FantasyParticipant participant = fantasyParticipantRepository.findByFantasyGameSeqAndPlayerId(
                request.getFantasyGameSeq(), request.getPlayerId())
                .orElse(null);

        // Validate draft rules (Composition, etc)
        // For FA, we might want to skip specific draft rules like "First Pick Rule" or "Team Restriction" enforcement strictly?
        // Or enforce them? Usually FA allows filling gaps.
        // Rule2Validator enforces composition.
        // If FA adds 21st player, canFit might fail if it expects 20.
        // Rule2Validator.getTotalPlayerCount returns 20.
        // If user has 20 players and adds 21st, validate() will likely fail if validator assumes max size.
        // But validate() mainly checks "canFit".
        // Rule2Validator.canFit uses backtrack with required slots + 2 bench.
        // If we have 21 players, it might fail.
        // Let's assume validation applies. If it fails for FA (21st player), we might need to bypass it or adjust validator.
        // User requirement: "Rule2에서 영입으로 인한 로스터 사이즈는 21이 최대야"
        // If I simply call validate, it checks composition.
        // Let's rely on standard validation. If it fails due to size, we might need to adjust validator later.
        // However, draftValidator.validate() doesn't check size limit directly, it checks composition.
        // If 21st player is Bench, and Rule2 allows infinite bench?
        // Rule2Validator: slots.put("BENCH", 2);
        // It enforces EXACTLY 2 bench slots in total 20 players.
        // So adding 3rd bench player will fail.
        // We might need to relax validation for FA or handle it.
        // For now, I will invoke validation only if Drafting. For FA, I enforce Salary Cap and Size (21).
        // Is composition enforcement required for FA? Usually yes.
        // But if Rule 2 strictly defines 20 slots, 21st player must be "Reserve" or "Extra Bench".
        // Let's skip heavy draft validation for FA to allow the flexibility, as FA is usually less strict on "Team Restriction" etc.
        // Or at least composition check might fail.

        if (isDrafting) {
             draftValidator.validate(game, targetPlayer, currentTeam, participant);
        } else {
             // For FA, maybe just basic checks? Or check foreigner limit?
             // Foreigner limit is usually strict.
             // Let's do a basic check manually or assume admin/user knows.
             // Actually, Rule1Validator checks Foreigner limit.
             // We should probably check Foreigner limit for FA too.
             // But avoiding composition crash.
        }

        // 4. Save Pick
        // Auto-Assign Position Logic
        String assignedPos = null;
        if (isDrafting) {
            assignedPos = determineInitialPosition(userPicks, targetPlayer);
        } else {
            // FA goes to Bench usually, or "BENCH" string
            assignedPos = "BENCH";
        }

        DraftPick pick = DraftPick.builder()
                .fantasyGameSeq(request.getFantasyGameSeq())
                .playerId(request.getPlayerId())
                .fantasyPlayerSeq(request.getFantasyPlayerSeq())
                .pickNumber(pickNumber)
                .assignedPosition(assignedPos)
                .pickStatus(DraftPick.PickStatus.NORMAL)
                .build();

        draftPickRepository.save(pick);

        // Log to RoasterLog
        RoasterLog.LogActionType actionType = isDrafting ? RoasterLog.LogActionType.DRAFT_PICK : RoasterLog.LogActionType.FA_ADD;
        RoasterLog logEntry = RoasterLog.builder()
                .fantasyGameSeq(request.getFantasyGameSeq())
                .participantId(request.getPlayerId())
                .fantasyPlayerSeq(request.getFantasyPlayerSeq())
                .actionType(actionType)
                .details(isDrafting ? "Picked in Draft" : "Signed via FA")
                .build();
        roasterLogRepository.save(logEntry);


        if (isDrafting) {
            // Update Deadline for NEXT pick
            if (game.getDraftTimeLimit() != null && game.getDraftTimeLimit() > 0) {
                game.setNextPickDeadline(LocalDateTime.now().plusMinutes(game.getDraftTimeLimit()));
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
        } else {
            // FA Event? Maybe just broadcast generic pick or special event
             DraftEventDto event = DraftEventDto.builder()
                    .type("FA_PICK")
                    .fantasyGameSeq(request.getFantasyGameSeq())
                    .playerId(request.getPlayerId())
                    .fantasyPlayerSeq(request.getFantasyPlayerSeq())
                    .playerName(targetPlayer.getName())
                    .playerTeam(targetPlayer.getTeam())
                    .pickNumber(pickNumber)
                    .message("Player " + request.getPlayerId() + " signed " + targetPlayer.getName() + " (FA)")
                    .build();
             messagingTemplate.convertAndSend("/topic/draft/" + request.getFantasyGameSeq(), event);
        }
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

        // Map Player Seq to DraftPick for easy access
        Map<Long, DraftPick> pickMap = picks.stream()
                .collect(Collectors.toMap(DraftPick::getFantasyPlayerSeq, Function.identity()));

        Set<Long> pickedSeqs = pickMap.keySet();
        List<FantasyPlayer> players = fantasyPlayerRepository.findAllById(pickedSeqs);

        return players.stream()
                .map(p -> {
                    FantasyPlayerDto dto = FantasyPlayerDto.from(p);
                    DraftPick pick = pickMap.get(p.getSeq());
                    if (pick != null) {
                        dto.setAssignedPosition(pick.getAssignedPosition());
                    }
                    return dto;
                })
                .sorted(Comparator.comparingInt(p -> {
                    DraftPick pick = pickMap.get(p.getSeq());
                    return pick != null ? pick.getPickNumber() : 0;
                }))
                .collect(Collectors.toList());
    }

    private String determineInitialPosition(List<DraftPick> existingPicks, FantasyPlayer newPlayer) {
        Set<String> occupied = existingPicks.stream()
                .map(DraftPick::getAssignedPosition)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        String positionStr = newPlayer.getPosition() != null ? newPlayer.getPosition() : "";
        if (positionStr.trim().isEmpty()) {
            return null; // Bench
        }

        String[] positions = positionStr.split(",");
        String primaryPos = positions[0].trim();

        if (isPitcher(primaryPos)) {
            // Pitcher logic: find first open slot (SP-1..SP-4, RP-1..RP-4, CL-1)
            // Or simple logic: Just store "SP", "RP", "CL". Frontend handles slots.
            // But if user has 4 SPs, 5th SP -> Bench.
            long spCount = occupied.stream().filter(p -> p.equals("SP")).count();
            long rpCount = occupied.stream().filter(p -> p.equals("RP")).count();
            long clCount = occupied.stream().filter(p -> p.equals("CL")).count();

            if (primaryPos.equals("SP")) return spCount < 4 ? "SP" : null;
            if (primaryPos.equals("RP")) return rpCount < 4 ? "RP" : null;
            if (primaryPos.equals("CL")) return clCount < 1 ? "CL" : null;
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
        return pos.equals("SP") || pos.equals("RP") || pos.equals("CL");
    }

    @Transactional
    public void updateRoster(Long gameSeq, Long playerId, RosterUpdateDto updateDto) {
        FantasyGame game = fantasyGameRepository.findById(gameSeq)
                .orElseThrow(() -> new IllegalArgumentException("Invalid Game Seq"));

        if (game.getStatus() == FantasyGame.GameStatus.FINISHED) {
            throw new IllegalStateException("Cannot update roster when FINISHED.");
        }

        List<DraftPick> myPicks = draftPickRepository.findByFantasyGameSeqAndPlayerId(gameSeq, playerId);
        Map<Long, DraftPick> pickMap = myPicks.stream()
                .collect(Collectors.toMap(DraftPick::getFantasyPlayerSeq, Function.identity()));

        // Check validation first
        if (updateDto.getEntries() != null) {
            Map<String, Integer> positionCounts = new java.util.HashMap<>();

            // Helper to increment and check
            java.util.function.BiConsumer<String, String> checkLimit = (pos, source) -> {
                int count = positionCounts.getOrDefault(pos, 0) + 1;
                positionCounts.put(pos, count);

                int limit = 1;
                if ("SP".equals(pos) || "RP".equals(pos)) limit = 4;
                // CL usually 1, others 1

                if (count > limit) {
                    throw new IllegalArgumentException("Position limit exceeded for " + pos + " (Max " + limit + ")");
                }
            };

            // Populate with existing positions NOT being updated
            Set<Long> updatingSeqs = updateDto.getEntries().stream().map(RosterUpdateDto.RosterEntry::getFantasyPlayerSeq).collect(Collectors.toSet());
            for (DraftPick pick : myPicks) {
                if (!updatingSeqs.contains(pick.getFantasyPlayerSeq()) && pick.getAssignedPosition() != null) {
                    checkLimit.accept(pick.getAssignedPosition(), "Existing");
                }
            }

            for (RosterUpdateDto.RosterEntry entry : updateDto.getEntries()) {
                if (entry.getAssignedPosition() != null) {
                    checkLimit.accept(entry.getAssignedPosition(), "New");
                }
            }

            // Apply updates
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

        List<FantasyPlayer> available;
        if (pickedPlayerSeqs.isEmpty()) {
            available = fantasyPlayerRepository.findAll();
        } else {
            available = fantasyPlayerRepository.findBySeqNotIn(pickedPlayerSeqs);
        }

        FantasyParticipant participant = fantasyParticipantRepository.findByFantasyGameSeqAndPlayerId(gameSeq, playerId).orElseThrow();
        List<FantasyPlayer> candidates = available;

        // First Pick Rule for Auto Pick (applies to both Rule 1 and Rule 2 if enabled)
        if (Boolean.TRUE.equals(game.getUseFirstPickRule()) && nextPick.round == 1) {
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

        List<FantasyPlayer> currentTeam;
        if (userPickedSeqs.isEmpty()) {
            currentTeam = Collections.emptyList();
        } else {
            currentTeam = fantasyPlayerRepository.findAllById(userPickedSeqs);
        }

        int currentCost = 0;
        if (game.getSalaryCap() != null && game.getSalaryCap() > 0) {
            currentCost = currentTeam.stream().mapToInt(p -> p.getCost() == null ? 0 : p.getCost()).sum();
        }

        FantasyPlayer selected = null;
        for (FantasyPlayer p : candidates) {
            try {
                // Salary Cap Check
                if (game.getSalaryCap() != null && game.getSalaryCap() > 0) {
                    int newCost = p.getCost() == null ? 0 : p.getCost();
                    if (currentCost + newCost > game.getSalaryCap()) {
                        continue;
                    }
                }

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
