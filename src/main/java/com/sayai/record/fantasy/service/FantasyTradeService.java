package com.sayai.record.fantasy.service;

import com.sayai.record.fantasy.entity.DraftPick;
import com.sayai.record.fantasy.entity.FantasyGame;
import com.sayai.record.fantasy.entity.FantasyParticipant;
import com.sayai.record.fantasy.entity.FantasyPlayer;
import com.sayai.record.fantasy.repository.DraftPickRepository;
import com.sayai.record.fantasy.repository.FantasyGameRepository;
import com.sayai.record.fantasy.repository.FantasyParticipantRepository;
import com.sayai.record.fantasy.repository.FantasyPlayerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class FantasyTradeService {

    private final DraftPickRepository draftPickRepository;
    private final FantasyGameRepository fantasyGameRepository;
    private final FantasyPlayerRepository fantasyPlayerRepository;
    private final FantasyParticipantRepository fantasyParticipantRepository;
    private final com.sayai.record.fantasy.repository.FantasyLogRepository fantasyLogRepository;
    private final com.sayai.record.fantasy.repository.FantasyTradeRepository fantasyTradeRepository;
    private final com.sayai.record.fantasy.repository.FantasyTradePlayerRepository fantasyTradePlayerRepository;

    @Transactional
    public void dropPlayer(Long gameSeq, Long playerId, Long fantasyPlayerSeq) {
        // 1. Check Game Status
        FantasyGame game = fantasyGameRepository.findById(gameSeq)
                .orElseThrow(() -> new IllegalArgumentException("Invalid Game Seq"));

        if (game.getStatus() != FantasyGame.GameStatus.ONGOING) {
            throw new IllegalStateException("선수를 방출할 수 없습니다. 게임이 진행중이 아닙니다.");
        }

        if (game.getRuleType() == FantasyGame.RuleType.RULE_1) {
            throw new IllegalStateException("Rule 1 게임에서는 방출 기능을 사용할 수 없습니다.");
        }

        // 2. Check Roster Size
        long rosterSize = draftPickRepository.countByFantasyGameSeqAndPlayerId(gameSeq, playerId);
        if (rosterSize < 19) {
            throw new IllegalStateException("선수단 규모가 19명 미만인 경우 방출할 수 없습니다.");
        }

        // 3. Find Draft Pick
        DraftPick pick = draftPickRepository.findByFantasyGameSeqAndPlayerIdAndFantasyPlayerSeq(gameSeq, playerId, fantasyPlayerSeq)
                .orElseThrow(() -> new IllegalArgumentException("보유하지 않은 선수입니다."));

        // 4. Check Bench Status
        String pos = pick.getAssignedPosition();
        if (pos != null && !pos.toUpperCase().contains("BENCH")) {
            throw new IllegalStateException("벤치 멤버만 방출할 수 있습니다.");
        }

        // 5. Drop (Delete Pick)
        draftPickRepository.delete(pick);

        // Log
        fantasyLogRepository.save(com.sayai.record.fantasy.entity.FantasyLog.builder()
                .fantasyGameSeq(gameSeq)
                .playerId(playerId)
                .fantasyPlayerSeq(fantasyPlayerSeq)
                .action(com.sayai.record.fantasy.entity.FantasyLog.ActionType.DROP)
                .isProcessed(false) // Explicitly set to false (default)
                .build());
    }

    @Transactional
    public void claimPlayer(Long gameSeq, Long playerId, Long fantasyPlayerSeq) {
        // 1. Check Game Status
        FantasyGame game = fantasyGameRepository.findById(gameSeq)
                .orElseThrow(() -> new IllegalArgumentException("Invalid Game Seq"));

        if (game.getStatus() != FantasyGame.GameStatus.ONGOING) {
            throw new IllegalStateException("Cannot claim player. Game is not ONGOING.");
        }

        if (game.getRuleType() == FantasyGame.RuleType.RULE_1) {
            throw new IllegalStateException("Rule 1 게임에서는 FA/웨이버 영입 기능을 사용할 수 없습니다.");
        }

        // 2. Check Availability
        if (draftPickRepository.existsByFantasyGameSeqAndFantasyPlayerSeq(gameSeq, fantasyPlayerSeq)) {
            throw new IllegalStateException("Player is already picked by another team.");
        }

        // --- NEW LOGIC: Check if player is on WAIVER (unprocessed DROP log) ---
        // FA Claim should fail if player is currently in Waiver period (i.e. has pending drop log)
        // because Waiver players are claimed via Admin only (per request 2)
        List<com.sayai.record.fantasy.entity.FantasyLog> pendingDrops = fantasyLogRepository.findByFantasyGameSeqAndFantasyPlayerSeqAndActionAndIsProcessedFalse(
                gameSeq, fantasyPlayerSeq, com.sayai.record.fantasy.entity.FantasyLog.ActionType.DROP);

        if (!pendingDrops.isEmpty()) {
            throw new IllegalStateException("Player is currently on WAIVER status and cannot be claimed as FA directly.");
        }
        // ----------------------------------------------------------------------

        FantasyPlayer player = fantasyPlayerRepository.findById(fantasyPlayerSeq)
                .orElseThrow(() -> new IllegalArgumentException("Player not found"));

        // 3. Check Salary Cap & Roster Size
        long currentRosterSize = draftPickRepository.countByFantasyGameSeqAndPlayerId(gameSeq, playerId);

        if (currentRosterSize >= 21) {
            throw new IllegalStateException("Your roster is full (Max 21 players). Drop a player first.");
        }

        List<DraftPick> myPicks = draftPickRepository.findByFantasyGameSeqAndPlayerId(gameSeq, playerId);

        // Calculate Current Cost
        Set<Long> myPickSeqs = myPicks.stream().map(DraftPick::getFantasyPlayerSeq).collect(Collectors.toSet());
        int currentCost = 0;
        if (!myPickSeqs.isEmpty()) {
            List<FantasyPlayer> myPlayers = fantasyPlayerRepository.findAllById(myPickSeqs);
            currentCost = myPlayers.stream().mapToInt(p -> p.getCost() == null ? 0 : p.getCost()).sum();
        }

        int playerCost = player.getCost() == null ? 0 : player.getCost();

        // Penalty Check (If size becomes 21)
        if (currentRosterSize >= 20) {
            playerCost += 5;
        }

        if (game.getSalaryCap() != null && game.getSalaryCap() > 0) {
            if (currentCost + playerCost > game.getSalaryCap()) {
                throw new IllegalStateException("Salary Cap Exceeded. Cost: " + (currentCost + playerCost) + " / " + game.getSalaryCap());
            }
        }

        // --- NEW LOGIC: REMOVED WAIVER ORDER UPDATE ---
        // Request: "웨이버 Claim 은 어드민화면에서만 ... FA 화면에서 선수 영입시에는 웨이버 순번이 밀릴 필요가 없어"
        // So we skip the waiver order reordering here.
        /*
        List<FantasyParticipant> participants = fantasyParticipantRepository.findByFantasyGameSeq(gameSeq);
        participants.sort(Comparator.comparingInt(p -> p.getWaiverOrder() == null ? 999 : p.getWaiverOrder()));

        FantasyParticipant me = participants.stream()
                .filter(p -> p.getPlayerId().equals(playerId))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Participant not found"));

        int myOldOrder = me.getWaiverOrder();
        int maxOrder = participants.size();

        for (FantasyParticipant p : participants) {
            if (p.getWaiverOrder() > myOldOrder) {
                p.setWaiverOrder(p.getWaiverOrder() - 1);
            }
        }
        me.setWaiverOrder(maxOrder);
        fantasyParticipantRepository.saveAll(participants);
        */
        // ----------------------------------------------

        // 5. Create Pick
        DraftPick newPick = DraftPick.builder()
                .fantasyGameSeq(gameSeq)
                .playerId(playerId)
                .fantasyPlayerSeq(fantasyPlayerSeq)
                .pickNumber(0)
                .assignedPosition("BENCH")
                .pickedAt(LocalDateTime.now())
                .build();

        draftPickRepository.save(newPick);

        // (Pending drops check is done above, but just in case of race condition or cleanup)
        // If we allowed claiming waiver via this method (which we blocked above), we would clear it.
        // Since we block it, no need to clear logs here.

        // Log
        fantasyLogRepository.save(com.sayai.record.fantasy.entity.FantasyLog.builder()
                .fantasyGameSeq(gameSeq)
                .playerId(playerId)
                .fantasyPlayerSeq(fantasyPlayerSeq)
                .action(com.sayai.record.fantasy.entity.FantasyLog.ActionType.CLAIM)
                .build());
    }

    @Transactional
    public void assignPlayerByAdmin(Long gameSeq, Long targetPlayerId, Long fantasyPlayerSeq, Long logSeq) {
        // 1. Check Game Status
        FantasyGame game = fantasyGameRepository.findById(gameSeq)
                .orElseThrow(() -> new IllegalArgumentException("Invalid Game Seq"));

        if (game.getStatus() != FantasyGame.GameStatus.ONGOING) {
            throw new IllegalStateException("Cannot assign player. Game is not ONGOING.");
        }

        if (game.getRuleType() == FantasyGame.RuleType.RULE_1) {
            throw new IllegalStateException("Rule 1 게임에서는 강제 이적 기능을 사용할 수 없습니다.");
        }

        // 2. Check Availability
        if (draftPickRepository.existsByFantasyGameSeqAndFantasyPlayerSeq(gameSeq, fantasyPlayerSeq)) {
            throw new IllegalStateException("Player is already picked by another team.");
        }

        FantasyPlayer player = fantasyPlayerRepository.findById(fantasyPlayerSeq)
                .orElseThrow(() -> new IllegalArgumentException("Player not found"));

        // 3. Check Roster Size & Salary Cap
        long currentRosterSize = draftPickRepository.countByFantasyGameSeqAndPlayerId(gameSeq, targetPlayerId);

        if (currentRosterSize >= 21) {
            throw new IllegalStateException("Target roster is full (Max 21 players).");
        }

        List<DraftPick> targetPicks = draftPickRepository.findByFantasyGameSeqAndPlayerId(gameSeq, targetPlayerId);
        Set<Long> targetPickSeqs = targetPicks.stream().map(DraftPick::getFantasyPlayerSeq).collect(Collectors.toSet());

        int currentCost = 0;
        if (!targetPickSeqs.isEmpty()) {
            List<FantasyPlayer> targetPlayers = fantasyPlayerRepository.findAllById(targetPickSeqs);
            currentCost = targetPlayers.stream().mapToInt(p -> p.getCost() == null ? 0 : p.getCost()).sum();
        }

        int playerCost = player.getCost() == null ? 0 : player.getCost();

        if (currentRosterSize == 20) {
            playerCost += 5;
        }

        if (game.getSalaryCap() != null && game.getSalaryCap() > 0) {
            if (currentCost + playerCost > game.getSalaryCap()) {
                throw new IllegalStateException("Salary Cap Exceeded for target team. Cost: " + (currentCost + playerCost) + " / " + game.getSalaryCap());
            }
        }

        // 4. Create Pick
        DraftPick newPick = DraftPick.builder()
                .fantasyGameSeq(gameSeq)
                .playerId(targetPlayerId)
                .fantasyPlayerSeq(fantasyPlayerSeq)
                .pickNumber(0)
                .assignedPosition("BENCH")
                .pickedAt(LocalDateTime.now())
                .build();

        draftPickRepository.save(newPick);

        // 5. Update Log if provided (Waiver assignment)
        if (logSeq != null) {
            fantasyLogRepository.findById(logSeq).ifPresent(log -> {
                log.setProcessed(true);
                fantasyLogRepository.save(log);
            });

            // --- UPDATE WAIVER ORDER HERE (Only for Admin Waiver Assign) ---
            // If this was a waiver claim (implied by logSeq existing and being a DROP log),
            // then we should update the waiver order of the target team.
            // Requirement 2 implies: "웨이버 Claim 은 어드민화면에서만 ... FA 화면에서 ... 밀릴 필요가 없어"
            // This suggests Waiver Claim (Admin Assign) SHOULD affect order?
            // Usually Waiver Claims reset priority. Let's assume Yes.

            List<FantasyParticipant> participants = fantasyParticipantRepository.findByFantasyGameSeq(gameSeq);
            participants.sort(Comparator.comparingInt(p -> p.getWaiverOrder() == null ? 999 : p.getWaiverOrder()));

            FantasyParticipant target = participants.stream()
                    .filter(p -> p.getPlayerId().equals(targetPlayerId))
                    .findFirst()
                    .orElse(null);

            if (target != null) {
                int oldOrder = target.getWaiverOrder();
                int maxOrder = participants.size();

                // Shift others
                for (FantasyParticipant p : participants) {
                    if (p.getWaiverOrder() > oldOrder) {
                        p.setWaiverOrder(p.getWaiverOrder() - 1);
                    }
                }
                target.setWaiverOrder(maxOrder);
                fantasyParticipantRepository.saveAll(participants);
            }
            // -------------------------------------------------------------
        }

        // Also ensure any other pending drops for this player are cleared
        List<com.sayai.record.fantasy.entity.FantasyLog> pendingDrops = fantasyLogRepository.findByFantasyGameSeqAndFantasyPlayerSeqAndActionAndIsProcessedFalse(
                gameSeq, fantasyPlayerSeq, com.sayai.record.fantasy.entity.FantasyLog.ActionType.DROP);
        for (com.sayai.record.fantasy.entity.FantasyLog log : pendingDrops) {
            if (!log.isProcessed()) {
                log.setProcessed(true);
                fantasyLogRepository.save(log);
            }
        }

        // 6. Log Assignment
        fantasyLogRepository.save(com.sayai.record.fantasy.entity.FantasyLog.builder()
                .fantasyGameSeq(gameSeq)
                .playerId(targetPlayerId)
                .fantasyPlayerSeq(fantasyPlayerSeq)
                .action(com.sayai.record.fantasy.entity.FantasyLog.ActionType.ADMIN_ASSIGN)
                .build());
    }

    @Transactional
    public void releaseToFa(Long logSeq) {
        if (logSeq == null) return;

        com.sayai.record.fantasy.entity.FantasyLog log = fantasyLogRepository.findById(logSeq)
                .orElseThrow(() -> new IllegalArgumentException("Log not found"));

        if (log.getAction() != com.sayai.record.fantasy.entity.FantasyLog.ActionType.DROP) {
            throw new IllegalArgumentException("Can only release DROP logs to FA.");
        }

        log.setProcessed(true);
        fantasyLogRepository.save(log);

        // Optionally log WAIVER_CLEARED, but updating isProcessed is sufficient for UI clearing.
    }

    @Transactional
    public void proposeTrade(Long playerId, com.sayai.record.fantasy.dto.TradeProposalDto dto) {
        FantasyGame game = fantasyGameRepository.findById(dto.getGameSeq())
                .orElseThrow(() -> new IllegalArgumentException("Invalid Game Seq"));

        if (game.getRuleType() == FantasyGame.RuleType.RULE_1) {
            throw new IllegalStateException("Rule 1 게임에서는 트레이드 기능을 사용할 수 없습니다.");
        }

        // 1. Create Trade
        com.sayai.record.fantasy.entity.FantasyTrade trade = com.sayai.record.fantasy.entity.FantasyTrade.builder()
                .fantasyGameSeq(dto.getGameSeq())
                .proposerId(playerId)
                .targetId(dto.getTargetPlayerId())
                .build();

        trade = fantasyTradeRepository.save(trade);

        // 2. Add Players
        for (Long seq : dto.getMyPlayers()) {
            DraftPick pick = draftPickRepository.findByFantasyGameSeqAndPlayerIdAndFantasyPlayerSeq(dto.getGameSeq(), playerId, seq)
                    .orElseThrow(() -> new IllegalArgumentException("You do not own player " + seq));

            if (pick.getAssignedPosition() == null || !pick.getAssignedPosition().toUpperCase().contains("BENCH")) {
                throw new IllegalStateException("Only BENCH players can be traded.");
            }

            fantasyTradePlayerRepository.save(com.sayai.record.fantasy.entity.FantasyTradePlayer.builder()
                    .fantasyTradeSeq(trade.getSeq())
                    .playerId(playerId)
                    .fantasyPlayerSeq(seq)
                    .build());
        }

        for (Long seq : dto.getTargetPlayers()) {
            DraftPick pick = draftPickRepository.findByFantasyGameSeqAndPlayerIdAndFantasyPlayerSeq(dto.getGameSeq(), dto.getTargetPlayerId(), seq)
                    .orElseThrow(() -> new IllegalArgumentException("Target does not own player " + seq));

            if (pick.getAssignedPosition() == null || !pick.getAssignedPosition().toUpperCase().contains("BENCH")) {
                throw new IllegalStateException("Target player must be on BENCH.");
            }

            fantasyTradePlayerRepository.save(com.sayai.record.fantasy.entity.FantasyTradePlayer.builder()
                    .fantasyTradeSeq(trade.getSeq())
                    .playerId(dto.getTargetPlayerId())
                    .fantasyPlayerSeq(seq)
                    .build());
        }
    }

    @Transactional
    public void approveTrade(Long tradeSeq) {
        com.sayai.record.fantasy.entity.FantasyTrade trade = fantasyTradeRepository.findById(tradeSeq)
                .orElseThrow(() -> new IllegalArgumentException("Trade not found"));

        if (trade.getStatus() != com.sayai.record.fantasy.entity.FantasyTrade.TradeStatus.PROPOSED) {
            throw new IllegalStateException("Trade is not in PROPOSED status.");
        }

        FantasyGame game = fantasyGameRepository.findById(trade.getFantasyGameSeq())
                .orElseThrow(() -> new IllegalArgumentException("Game not found"));

        List<com.sayai.record.fantasy.entity.FantasyTradePlayer> tradePlayers = fantasyTradePlayerRepository.findByFantasyTradeSeq(tradeSeq);

        List<Long> proposerOut = tradePlayers.stream().filter(p -> p.getPlayerId().equals(trade.getProposerId())).map(com.sayai.record.fantasy.entity.FantasyTradePlayer::getFantasyPlayerSeq).collect(Collectors.toList());
        List<Long> targetOut = tradePlayers.stream().filter(p -> p.getPlayerId().equals(trade.getTargetId())).map(com.sayai.record.fantasy.entity.FantasyTradePlayer::getFantasyPlayerSeq).collect(Collectors.toList());

        validateTradeImpact(game, trade.getProposerId(), proposerOut, targetOut);
        validateTradeImpact(game, trade.getTargetId(), targetOut, proposerOut);

        for (com.sayai.record.fantasy.entity.FantasyTradePlayer tp : tradePlayers) {
            Long ownerId = tp.getPlayerId();
            Long newOwnerId = ownerId.equals(trade.getProposerId()) ? trade.getTargetId() : trade.getProposerId();

            DraftPick pick = draftPickRepository.findByFantasyGameSeqAndPlayerIdAndFantasyPlayerSeq(game.getSeq(), ownerId, tp.getFantasyPlayerSeq())
                    .orElseThrow(() -> new IllegalStateException("Player not found during execution"));

            pick.setPlayerId(newOwnerId);
            pick.setAssignedPosition("BENCH");
            draftPickRepository.save(pick);

            fantasyLogRepository.save(com.sayai.record.fantasy.entity.FantasyLog.builder()
                    .fantasyGameSeq(game.getSeq())
                    .playerId(newOwnerId)
                    .fantasyPlayerSeq(tp.getFantasyPlayerSeq())
                    .action(com.sayai.record.fantasy.entity.FantasyLog.ActionType.TRADE)
                    .build());
        }

        trade.setStatus(com.sayai.record.fantasy.entity.FantasyTrade.TradeStatus.COMPLETED);
        fantasyTradeRepository.save(trade);
    }

    @Transactional
    public void rejectTrade(Long tradeSeq) {
        com.sayai.record.fantasy.entity.FantasyTrade trade = fantasyTradeRepository.findById(tradeSeq)
                .orElseThrow(() -> new IllegalArgumentException("Trade not found"));
        trade.setStatus(com.sayai.record.fantasy.entity.FantasyTrade.TradeStatus.REJECTED);
        fantasyTradeRepository.save(trade);
    }

    private void validateTradeImpact(FantasyGame game, Long playerId, List<Long> losing, List<Long> gaining) {
        List<DraftPick> currentPicks = draftPickRepository.findByFantasyGameSeqAndPlayerId(game.getSeq(), playerId);

        long newSize = currentPicks.size() - losing.size() + gaining.size();

        Set<Long> currentSeqs = currentPicks.stream().map(DraftPick::getFantasyPlayerSeq).collect(Collectors.toSet());
        currentSeqs.removeAll(losing);
        currentSeqs.addAll(gaining);

        int cost = 0;
        if (!currentSeqs.isEmpty()) {
            List<FantasyPlayer> players = fantasyPlayerRepository.findAllById(currentSeqs);
            cost = players.stream().mapToInt(p -> p.getCost() == null ? 0 : p.getCost()).sum();
        }

        if (newSize == 21) {
            cost += 5;
        }

        if (game.getSalaryCap() != null && game.getSalaryCap() > 0) {
            if (cost > game.getSalaryCap()) {
                throw new IllegalStateException("Trade would exceed salary cap for player " + playerId + ". New Cost: " + cost);
            }
        }
    }

    @Transactional(readOnly = true)
    public List<com.sayai.record.fantasy.dto.TradeLogDto> getTradeRequests(Long gameSeq) {
        List<com.sayai.record.fantasy.entity.FantasyTrade> trades = fantasyTradeRepository.findByFantasyGameSeqAndStatus(gameSeq, com.sayai.record.fantasy.entity.FantasyTrade.TradeStatus.PROPOSED);

        return trades.stream().map(t -> {
            List<com.sayai.record.fantasy.entity.FantasyTradePlayer> tps = fantasyTradePlayerRepository.findByFantasyTradeSeq(t.getSeq());

            Set<Long> pIds = tps.stream().map(com.sayai.record.fantasy.entity.FantasyTradePlayer::getFantasyPlayerSeq).collect(Collectors.toSet());
            java.util.Map<Long, FantasyPlayer> pMap = fantasyPlayerRepository.findAllById(pIds).stream().collect(Collectors.toMap(FantasyPlayer::getSeq, java.util.function.Function.identity()));

            String proposerName = fantasyParticipantRepository.findByFantasyGameSeqAndPlayerId(gameSeq, t.getProposerId()).map(FantasyParticipant::getTeamName).orElse("Unknown");
            String targetName = fantasyParticipantRepository.findByFantasyGameSeqAndPlayerId(gameSeq, t.getTargetId()).map(FantasyParticipant::getTeamName).orElse("Unknown");

            String fromPlayers = tps.stream().filter(x -> x.getPlayerId().equals(t.getProposerId()))
                    .map(x -> pMap.get(x.getFantasyPlayerSeq()).getName()).collect(Collectors.joining(", "));

            String toPlayers = tps.stream().filter(x -> x.getPlayerId().equals(t.getTargetId()))
                    .map(x -> pMap.get(x.getFantasyPlayerSeq()).getName()).collect(Collectors.joining(", "));

            return com.sayai.record.fantasy.dto.TradeLogDto.builder()
                    .tradeSeq(t.getSeq())
                    .proposerTeam(proposerName)
                    .targetTeam(targetName)
                    .proposerPlayers(fromPlayers)
                    .targetPlayers(toPlayers)
                    .status(t.getStatus().name())
                    .createdAt(t.getCreatedAt())
                    .build();
        }).collect(Collectors.toList());
    }
}
