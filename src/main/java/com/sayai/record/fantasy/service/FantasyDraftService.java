package com.sayai.record.fantasy.service;

import com.sayai.kbo.model.KboHitterStats;
import com.sayai.kbo.model.KboPitcherStats;
import com.sayai.kbo.repository.KboHitterStatsRepository;
import com.sayai.kbo.repository.KboPitcherStatsRepository;
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

import java.time.LocalDate;
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
    private final RosterLogRepository rosterLogRepository;
    private final DraftValidator draftValidator;
    private final SimpMessagingTemplate messagingTemplate;
    private final ObjectProvider<DraftScheduler> draftSchedulerProvider;
    private final KboHitterStatsRepository kboHitterStatsRepository;
    private final KboPitcherStatsRepository kboPitcherStatsRepository;

    private static final Set<String> PITCHER_POSITIONS = Set.of("SP", "RP", "CL");

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

        List<FantasyPlayer> availablePlayers = filteredPlayers.stream()
                .filter(p -> !pickedPlayerSeqs.contains(p.getSeq()))
                .collect(Collectors.toList());

        // Bulk fetch current-season stats (2 queries total regardless of player count)
        int currentSeason = LocalDate.now().getYear();
        Set<Long> playerSeqs = availablePlayers.stream().map(FantasyPlayer::getSeq).collect(Collectors.toSet());

        Map<Long, KboHitterStats> hitterStatsMap = kboHitterStatsRepository
                .findByPlayerIdInAndSeason(playerSeqs, currentSeason)
                .stream()
                .collect(Collectors.toMap(KboHitterStats::getPlayerId, Function.identity()));

        Map<Long, KboPitcherStats> pitcherStatsMap = kboPitcherStatsRepository
                .findByPlayerIdInAndSeason(playerSeqs, currentSeason)
                .stream()
                .collect(Collectors.toMap(KboPitcherStats::getPlayerId, Function.identity()));

        return availablePlayers.stream()
                .map(p -> {
                    FantasyPlayerDto dto = FantasyPlayerDto.from(p);
                    String primaryPos = p.getPosition() != null ? p.getPosition().split(",")[0].trim() : "";
                    if (PITCHER_POSITIONS.contains(primaryPos)) {
                        KboPitcherStats ps = pitcherStatsMap.get(p.getSeq());
                        if (ps != null) {
                            dto.setStats(ps.getWin() + "승, " + ps.getEra() + " ERA, " + ps.getWhip() + " WHIP");
                        }
                    } else {
                        KboHitterStats hs = hitterStatsMap.get(p.getSeq());
                        if (hs != null) {
                            dto.setStats(hs.getAvg() + ", " + hs.getHr() + "홈런, " + hs.getRbi() + "타점, " + hs.getSb() + "도루");
                        }
                    }
                    return dto;
                })
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
            // Max 21 allowed for FA
            int limit = 21;
            if (userPicks.size() >= limit) {
                throw new IllegalStateException("Roster full (Max " + limit + ")");
            }
        }

        // Get Participant Info
        FantasyParticipant participant = fantasyParticipantRepository.findByFantasyGameSeqAndPlayerId(
                request.getFantasyGameSeq(), request.getPlayerId())
                .orElse(null);

        // Salary Cap Check
        if (game.getSalaryCap() != null && game.getSalaryCap() > 0) {
            List<FantasyPlayer> hypotheticalTeam = new java.util.ArrayList<>(currentTeam);
            hypotheticalTeam.add(targetPlayer);
            int projectedCost = com.sayai.record.fantasy.util.SalaryCapCalculator.calculateTeamCost(game, participant, hypotheticalTeam).getTotalCost();

            // Penalty check for 21st player
            int penalty = 0;
            if (isFA && userPicks.size() == 20) {
                penalty = 5;
            }

            if (projectedCost + penalty > game.getSalaryCap()) {
                throw new IllegalStateException("샐캡 초과: " + (projectedCost + penalty) + " / " + game.getSalaryCap());
            }
        }

        // Validate draft rules (Composition, etc) ONLY for DRAFTING
        if (isDrafting) {
             draftValidator.validate(game, targetPlayer, currentTeam, participant);
        }

        // 4. Save Pick
        // Auto-Assign Position Logic
        String assignedPos = determineInitialPosition(userPicks, targetPlayer);

        DraftPick pick = DraftPick.builder()
                .fantasyGameSeq(request.getFantasyGameSeq())
                .playerId(request.getPlayerId())
                .fantasyPlayerSeq(request.getFantasyPlayerSeq())
                .pickNumber(pickNumber)
                .assignedPosition(assignedPos)
                .pickStatus(DraftPick.PickStatus.NORMAL)
                .build();

        draftPickRepository.save(pick);

        // Log to RosterLog
        RosterLog.LogActionType actionType = isDrafting ? RosterLog.LogActionType.DRAFT_PICK : RosterLog.LogActionType.FA_ADD;
        String logDetails = isDrafting ? "Draft Pick #" + pickNumber + (request.isAutoPick() ? " (Auto)" : "") : targetPlayer.getName() + " - Signed via FA";

        RosterLog logEntry = RosterLog.builder()
                .fantasyGameSeq(request.getFantasyGameSeq())
                .participantId(request.getPlayerId())
                .fantasyPlayerSeq(request.getFantasyPlayerSeq())
                .actionType(actionType)
                .details(logDetails)
                .build();
        rosterLogRepository.save(logEntry);


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
            // FA Event: Do not broadcast WebSocket message as requested
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
    public com.sayai.record.fantasy.dto.MyPicksResponseDto getPickedPlayers(Long gameSeq, Long playerId) {
        // Fetch picks
        List<DraftPick> picks = draftPickRepository.findByFantasyGameSeqAndPlayerId(gameSeq, playerId).stream()
                .filter(pick -> pick.getPickStatus() != DraftPick.PickStatus.WAIVER_REQ)
                .collect(Collectors.toList());

        // Map Player Seq to DraftPick for easy access
        Map<Long, DraftPick> pickMap = picks.stream()
                .collect(Collectors.toMap(DraftPick::getFantasyPlayerSeq, Function.identity()));

        Set<Long> pickedSeqs = pickMap.keySet();
        List<FantasyPlayer> players = fantasyPlayerRepository.findAllById(pickedSeqs);

        // Fetch participant and game to calculate current cost with potential discount
        FantasyGame game = fantasyGameRepository.findById(gameSeq)
                .orElseThrow(() -> new IllegalArgumentException("Invalid game"));
        FantasyParticipant participant = fantasyParticipantRepository.findByFantasyGameSeqAndPlayerId(gameSeq, playerId)
                .orElse(null);

        com.sayai.record.fantasy.util.SalaryCapResult capResult = null;
        if (participant != null) {
            capResult = com.sayai.record.fantasy.util.SalaryCapCalculator.calculateTeamCost(game, participant, players);
        }

        final com.sayai.record.fantasy.util.SalaryCapResult finalCapResult = capResult;

        List<FantasyPlayerDto> dtoList = players.stream()
                .map(p -> {
                    FantasyPlayerDto dto = FantasyPlayerDto.from(p);
                    DraftPick pick = pickMap.get(p.getSeq());
                    if (pick != null) {
                        dto.setAssignedPosition(pick.getAssignedPosition());
                        dto.setOwnerId(pick.getPlayerId());
                        dto.setPickStatus(pick.getPickStatus() != null ? pick.getPickStatus().name() : "NORMAL");
                    }
                    if (finalCapResult != null && p.getSeq().equals(finalCapResult.getDiscountedPlayerSeq())) {
                        dto.setDiscounted(true);
                        dto.setCost(finalCapResult.getDiscountedCost());
                    }
                    return dto;
                })
                .sorted(Comparator.comparingInt(p -> {
                    DraftPick pick = pickMap.get(p.getSeq());
                    return pick != null ? pick.getPickNumber() : 0;
                }))
                .collect(Collectors.toList());

        int currentCost = (capResult != null) ? capResult.getTotalCost() :
            players.stream().mapToInt(p -> p.getCost() == null ? 0 : p.getCost()).sum();

        return new com.sayai.record.fantasy.dto.MyPicksResponseDto(dtoList, currentCost);
    }

    private String determineInitialPosition(List<DraftPick> existingPicks, FantasyPlayer newPlayer) {
        Map<String, Long> occupiedCounts = existingPicks.stream()
                .filter(p -> p.getAssignedPosition() != null)
                .collect(Collectors.groupingBy(DraftPick::getAssignedPosition, Collectors.counting()));

        String positionStr = newPlayer.getPosition() != null ? newPlayer.getPosition() : "";
        if (positionStr.trim().isEmpty()) {
            return "BENCH";
        }

        String[] positions = positionStr.split(",");
        String primaryPos = positions[0].trim();

        if (isPitcher(primaryPos)) {
            long spCount = occupiedCounts.getOrDefault("SP", 0L);
            long rpCount = occupiedCounts.getOrDefault("RP", 0L);
            long clCount = occupiedCounts.getOrDefault("CL", 0L);

            if (primaryPos.equals("SP")) return spCount < 4 ? "SP" : "BENCH";
            if (primaryPos.equals("RP")) return rpCount < 4 ? "RP" : "BENCH";
            if (primaryPos.equals("CL")) return clCount < 1 ? "CL" : "BENCH";
            return "BENCH";
        } else {
            // Batter Logic: 주포지션 → 부포지션 → DH → BENCH
            for (String p : positions) {
                String pos = p.trim();
                if (!pos.isEmpty() && occupiedCounts.getOrDefault(pos, 0L) == 0) {
                    return pos;
                }
            }
            if (occupiedCounts.getOrDefault("DH", 0L) == 0) {
                return "DH";
            }
            return "BENCH";
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

        if (game.getSalaryCap() != null && game.getSalaryCap() > 0) {
            Set<Long> pickedSeqs = myPicks.stream()
                    .map(DraftPick::getFantasyPlayerSeq)
                    .collect(Collectors.toSet());
            List<FantasyPlayer> currentTeam = fantasyPlayerRepository.findAllById(pickedSeqs);
            FantasyParticipant participant = fantasyParticipantRepository.findByFantasyGameSeqAndPlayerId(gameSeq, playerId).orElse(null);

            int currentCost = com.sayai.record.fantasy.util.SalaryCapCalculator.calculateTeamCost(game, participant, currentTeam).getTotalCost();
            if (currentCost > game.getSalaryCap()) {
                throw new IllegalStateException("샐러리캡을 초과하여 저장할 수 없습니다. (현재: " + currentCost + " / 제한: " + game.getSalaryCap() + ")");
            }
        }

        // Check validation first
        if (updateDto.getEntries() != null) {
            Map<String, Integer> positionCounts = new java.util.HashMap<>();

            // Helper to increment and check
            java.util.function.BiConsumer<String, String> checkLimit = (pos, source) -> {
                if ("BENCH".equals(pos)) return;

                int count = positionCounts.getOrDefault(pos, 0) + 1;
                positionCounts.put(pos, count);

                int limit;
                if ("SP".equals(pos) || "RP".equals(pos)) limit = 4;
                else limit = 1;

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
                    String pos = entry.getAssignedPosition();
                    if (pos != null && pos.startsWith("BENCH")) {
                        pos = "BENCH";
                    }
                    pick.setAssignedPosition(pos);
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
            available = fantasyPlayerRepository.findAllActivePlayers();
        } else {
            available = fantasyPlayerRepository.findBySeqNotIn(pickedPlayerSeqs);
        }

        FantasyParticipant participant = fantasyParticipantRepository.findByFantasyGameSeqAndPlayerId(gameSeq, playerId).orElse(null);
        if (participant == null) participant = FantasyParticipant.builder().playerId(playerId).build();

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
            currentCost = com.sayai.record.fantasy.util.SalaryCapCalculator.calculateTeamCost(game, participant, currentTeam).getTotalCost();
        }

        FantasyPlayer selected = null;
        for (FantasyPlayer p : candidates) {
            try {
                // Salary Cap Check
                if (game.getSalaryCap() != null && game.getSalaryCap() > 0) {
                    List<FantasyPlayer> hypotheticalTeam = new java.util.ArrayList<>(currentTeam);
                    hypotheticalTeam.add(p);
                    int projectedCost = com.sayai.record.fantasy.util.SalaryCapCalculator.calculateTeamCost(game, participant, hypotheticalTeam).getTotalCost();

                    if (projectedCost > game.getSalaryCap()) {
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
            req.setAutoPick(true);
            draftPlayer(req);
        } else {
            // Log or handle no valid pick found
            log.error("AutoPick failed: No valid player found for game {} user {}", gameSeq, playerId);
        }
    }
}
