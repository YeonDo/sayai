package com.sayai.record.fantasy.service;

import com.sayai.record.admin.dto.AdminRosterTransactionDto;
import com.sayai.record.fantasy.dto.TradeBoardDto;
import com.sayai.record.fantasy.dto.WaiverBoardDto;
import com.sayai.record.fantasy.entity.*;
import com.sayai.record.fantasy.repository.*;
import com.sayai.record.firebase.FcmService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class FantasyRosterService {

    private final DraftPickRepository draftPickRepository;
    private final FantasyGameRepository fantasyGameRepository;
    private final RosterTransactionRepository rosterTransactionRepository;
    private final RosterLogRepository rosterLogRepository;
    private final FantasyPlayerRepository fantasyPlayerRepository;
    private final FantasyParticipantRepository fantasyParticipantRepository;
    private final FantasyWaiverClaimRepository waiverClaimRepository;
    private final FantasyWaiverOrderRepository waiverOrderRepository;
    private final TradeBoardRepository tradeBoardRepository;
    private final FcmService fcmService;

    // --- Waiver Logic ---

    @Transactional(readOnly = true)
    public List<com.sayai.record.fantasy.dto.WaiverOrderDto> getWaiverOrderList(Long gameSeq) {
        List<FantasyWaiverOrder> orders = waiverOrderRepository.findByGameSeqOrderByOrderNumAsc(gameSeq);
        if (orders.isEmpty()) {
            return new ArrayList<>();
        }

        Set<Long> playerIds = orders.stream().map(FantasyWaiverOrder::getPlayerId).collect(Collectors.toSet());
        java.util.Map<Long, String> teamMap = fantasyParticipantRepository.findByFantasyGameSeqAndPlayerIdIn(gameSeq, playerIds)
                .stream().collect(Collectors.toMap(FantasyParticipant::getPlayerId, FantasyParticipant::getTeamName, (a, b) -> a));

        return orders.stream().map(o -> com.sayai.record.fantasy.dto.WaiverOrderDto.builder()
                .teamName(teamMap.getOrDefault(o.getPlayerId(), "Unknown Team"))
                .orderNum(o.getOrderNum())
                .build()).collect(Collectors.toList());
    }

    public List<WaiverBoardDto> getWaiverBoard(Long gameSeq) {
        List<RosterTransaction> waivers = rosterTransactionRepository.findByFantasyGameSeqAndStatusAndType(
                gameSeq, RosterTransaction.TransactionStatus.REQUESTED, RosterTransaction.TransactionType.WAIVER);

        if (waivers.isEmpty()) {
            return new ArrayList<>();
        }

        Set<Long> requesterIds = waivers.stream().map(RosterTransaction::getRequesterId).collect(Collectors.toSet());
        Set<Long> playerSeqs = waivers.stream()
                .flatMap(tx -> getSeqsStream(tx.getGivingPlayerSeqs(), "WaiverBoard preload tx:" + tx.getSeq()))
                .collect(Collectors.toSet());
        List<Long> transactionSeqs = waivers.stream().map(RosterTransaction::getSeq).collect(Collectors.toList());

        java.util.Map<Long, String> requesterTeamMap = fantasyParticipantRepository.findByFantasyGameSeqAndPlayerIdIn(gameSeq, requesterIds)
                .stream().collect(Collectors.toMap(FantasyParticipant::getPlayerId, FantasyParticipant::getTeamName, (a, b) -> a));

        java.util.Map<Long, FantasyPlayer> playerMap = fantasyPlayerRepository.findAllById(playerSeqs)
                .stream().collect(Collectors.toMap(FantasyPlayer::getSeq, p -> p));

        java.util.Map<Long, List<FantasyWaiverClaim>> claimMap = waiverClaimRepository.findByWaiverSeqIn(transactionSeqs)
                .stream().collect(Collectors.groupingBy(FantasyWaiverClaim::getWaiverSeq));

        return waivers.stream().map(tx -> {
            String requesterTeam = requesterTeamMap.getOrDefault(tx.getRequesterId(), "Unknown");

            Long playerSeq = getSeqsStream(tx.getGivingPlayerSeqs(), "WaiverBoard tx:" + tx.getSeq()).findFirst().orElse(null);
            if (playerSeq == null) return null;

            FantasyPlayer player = playerMap.get(playerSeq);
            if (player == null) return null;

            List<FantasyWaiverClaim> claims = claimMap.get(tx.getSeq());
            List<Long> claimIds = claims != null ? claims.stream().map(FantasyWaiverClaim::getClaimPlayerId).collect(Collectors.toList()) : new java.util.ArrayList<>();

            return WaiverBoardDto.builder()
                    .transactionSeq(tx.getSeq())
                    .requesterId(tx.getRequesterId())
                    .requesterTeamName(requesterTeam)
                    .playerName(player.getName())
                    .playerTeam(player.getTeam())
                    .playerPosition(player.getPosition())
                    .playerCost(player.getCost())
                    .waiverDate(tx.getCreatedAt())
                    .claimPlayerIds(claimIds)
                    .build();
        }).filter(java.util.Objects::nonNull).collect(Collectors.toList());
    }

    @Transactional
    public void claimWaiver(Long gameSeq, Long transactionSeq, Long claimerId) {
        RosterTransaction tx = rosterTransactionRepository.findById(transactionSeq)
                .orElseThrow(() -> new IllegalArgumentException("Transaction not found"));

        if (tx.getType() != RosterTransaction.TransactionType.WAIVER || tx.getStatus() != RosterTransaction.TransactionStatus.REQUESTED) {
            throw new IllegalArgumentException("Invalid waiver transaction");
        }

        if (tx.getRequesterId().equals(claimerId)) {
            throw new IllegalArgumentException("Cannot claim your own waived player");
        }

        waiverOrderRepository.findByGameSeqAndPlayerId(gameSeq, claimerId)
                .orElseThrow(() -> new IllegalArgumentException("User not participating in this game"));

        waiverClaimRepository.save(FantasyWaiverClaim.builder()
                .waiverSeq(transactionSeq)
                .claimPlayerId(claimerId)
                .build());
    }

    @Transactional
    public void requestWaiver(Long gameSeq, Long requesterId, Long fantasyPlayerSeq) {
        DraftPick pick = draftPickRepository.findByFantasyGameSeqAndFantasyPlayerSeq(gameSeq, fantasyPlayerSeq)
                .orElseThrow(() -> new IllegalArgumentException("Player not found in game"));

        if (!pick.getPlayerId().equals(requesterId)) {
             throw new IllegalArgumentException("Player does not belong to you");
        }

        if (pick.getPickStatus() != DraftPick.PickStatus.NORMAL) {
            throw new IllegalStateException("Player is already in " + pick.getPickStatus() + " state");
        }

        pick.setPickStatus(DraftPick.PickStatus.WAIVER_REQ);
        pick.setAssignedPosition("BENCH");
        draftPickRepository.save(pick);

        // Create Transaction
        RosterTransaction tx = RosterTransaction.builder()
                .fantasyGameSeq(gameSeq)
                .requesterId(requesterId)
                .type(RosterTransaction.TransactionType.WAIVER)
                .status(RosterTransaction.TransactionStatus.REQUESTED)
                .givingPlayerSeqs(String.valueOf(fantasyPlayerSeq))
                .build();
        rosterTransactionRepository.save(tx);

        // Log
        FantasyPlayer player = fantasyPlayerRepository.findById(fantasyPlayerSeq).orElseThrow();
        logAction(gameSeq, requesterId, fantasyPlayerSeq, RosterLog.LogActionType.WAIVER_RELEASE, player.getName() + " - Waiver Requested");

        // Send FCM message
        FantasyParticipant requester = fantasyParticipantRepository.findByFantasyGameSeqAndPlayerId(gameSeq, requesterId).orElse(null);
        String teamName = requester != null && requester.getTeamName() != null ? requester.getTeamName() : "Unknown Team";
        String body = String.format("%s팀에서 웨이버를 신청했습니다: %s (%s, %s)",
                teamName, player.getName(), player.getTeam(), player.getPosition());
        fcmService.sendTopicMessage("game_" + gameSeq, "웨이버 신청", body);
    }

    @Transactional
    public void processWaiver(Long transactionSeq, String decision, Long targetParticipantId) {
        RosterTransaction tx = rosterTransactionRepository.findById(transactionSeq)
                .orElseThrow(() -> new IllegalArgumentException("Transaction not found"));

        if (tx.getType() != RosterTransaction.TransactionType.WAIVER) {
            throw new IllegalArgumentException("Not a Waiver transaction");
        }
        if (tx.getStatus() != RosterTransaction.TransactionStatus.REQUESTED) {
            throw new IllegalStateException("Transaction is already processed");
        }

        Long playerSeq = getSeqsStream(tx.getGivingPlayerSeqs(), "processWaiver tx:" + tx.getSeq()).findFirst()
                .orElseThrow(() -> new IllegalStateException("Invalid player sequence: " + tx.getGivingPlayerSeqs()));
        FantasyPlayer player = fantasyPlayerRepository.findById(playerSeq).orElseThrow();
        DraftPick pick = draftPickRepository.findByFantasyGameSeqAndFantasyPlayerSeq(tx.getFantasyGameSeq(), playerSeq)
                .orElseThrow(() -> new IllegalStateException("Draft pick not found for player " + playerSeq));

        if ("CLAIM".equalsIgnoreCase(decision)) {
            if (targetParticipantId == null) {
                throw new IllegalArgumentException("Target Participant ID required for CLAIM");
            }
            // Move to Target
            pick.setPlayerId(targetParticipantId);
            pick.setPickStatus(DraftPick.PickStatus.NORMAL);
            pick.setAssignedPosition("BENCH"); // Reset position to Bench for new owner
            draftPickRepository.save(pick);

            tx.setStatus(RosterTransaction.TransactionStatus.APPROVED);
            tx.setTargetId(targetParticipantId);

            FantasyParticipant targetName = fantasyParticipantRepository.findByFantasyGameSeqAndPlayerId(tx.getFantasyGameSeq(), targetParticipantId).orElseGet(null);
            String targetTeamName = targetName.getTeamName();

            logAction(tx.getFantasyGameSeq(), targetParticipantId, playerSeq, RosterLog.LogActionType.WAIVER_CLAIM, player.getName() + " - Claimed by " + targetTeamName);

        } else if ("FA".equalsIgnoreCase(decision)) {
            // Move to FA (Delete Pick)
            draftPickRepository.delete(pick);

            tx.setStatus(RosterTransaction.TransactionStatus.FA_MOVED);

            logAction(tx.getFantasyGameSeq(), tx.getRequesterId(), playerSeq, RosterLog.LogActionType.WAIVER_FA, player.getName() + " - Moved to FA");

        } else {
            throw new IllegalArgumentException("Invalid decision: " + decision);
        }

        rosterTransactionRepository.save(tx);
    }

    // --- Trade Logic ---

    @Transactional
    public void requestTrade(Long gameSeq, Long requesterId, Long targetId, List<Long> givingPlayerSeqs, List<Long> receivingPlayerSeqs, String comment) {
        if (givingPlayerSeqs == null || givingPlayerSeqs.isEmpty()) {
            throw new IllegalArgumentException("Must give at least one player");
        }
        if (receivingPlayerSeqs == null || receivingPlayerSeqs.isEmpty()) {
            throw new IllegalArgumentException("Must receive at least one player");
        }
        if (givingPlayerSeqs.size() > 2 || receivingPlayerSeqs.size() > 2) {
            throw new IllegalArgumentException("Max 2 players per side");
        }

        List<DraftPick> givingPicks = validateTradePlayers(gameSeq, requesterId, givingPlayerSeqs);
        List<DraftPick> receivingPicks = validateTradePlayers(gameSeq, targetId, receivingPlayerSeqs);

        for (DraftPick p : givingPicks) {
            p.setPickStatus(DraftPick.PickStatus.TRADE_PENDING);
        }
        for (DraftPick p : receivingPicks) {
            p.setPickStatus(DraftPick.PickStatus.TRADE_PENDING);
        }
        draftPickRepository.saveAll(givingPicks);
        draftPickRepository.saveAll(receivingPicks);

        RosterTransaction tx = RosterTransaction.builder()
                .fantasyGameSeq(gameSeq)
                .requesterId(requesterId)
                .targetId(targetId)
                .type(RosterTransaction.TransactionType.TRADE)
                .status(RosterTransaction.TransactionStatus.SUGGESTED)
                .givingPlayerSeqs(givingPlayerSeqs.stream().map(String::valueOf).collect(Collectors.joining(",")))
                .receivingPlayerSeqs(receivingPlayerSeqs.stream().map(String::valueOf).collect(Collectors.joining(",")))
                .comment(comment)
                .build();
        rosterTransactionRepository.save(tx);

        FantasyParticipant requester = fantasyParticipantRepository.findByFantasyGameSeqAndPlayerId(gameSeq, requesterId).orElse(null);
        FantasyParticipant target = fantasyParticipantRepository.findByFantasyGameSeqAndPlayerId(gameSeq, targetId).orElse(null);
        String requesterTeam = requester != null ? requester.getTeamName() : "Unknown";
        String targetTeam = target != null ? target.getTeamName() : "Unknown";

        String givingDetails = fantasyPlayerRepository.findAllById(givingPlayerSeqs).stream()
                .map(fp -> String.format("%s (%s)", fp.getName(), fp.getTeam()))
                .collect(Collectors.joining(", "));
        String receivingDetails = fantasyPlayerRepository.findAllById(receivingPlayerSeqs).stream()
                .map(fp -> String.format("%s (%s)", fp.getName(), fp.getTeam()))
                .collect(Collectors.joining(", "));

        String body = String.format("%s팀이 트레이드를 제안했습니다.\n주는 선수: %s\n받는 선수: %s",
                requesterTeam, givingDetails, receivingDetails);
        fcmService.sendTopicMessage("user_" + targetId + "_game_" + gameSeq, "트레이드 제안", body);
    }

    @Transactional
    public void respondToTrade(Long transactionSeq, Long responderId, boolean accept) {
        RosterTransaction tx = rosterTransactionRepository.findById(transactionSeq)
                .orElseThrow(() -> new IllegalArgumentException("Transaction not found"));

        if (tx.getType() != RosterTransaction.TransactionType.TRADE) {
            throw new IllegalArgumentException("Not a Trade transaction");
        }
        if (tx.getStatus() != RosterTransaction.TransactionStatus.SUGGESTED) {
            throw new IllegalStateException("Trade is not in SUGGESTED status");
        }

        boolean isRequester = tx.getRequesterId().equals(responderId);
        boolean isTarget = tx.getTargetId().equals(responderId);

        if (!isRequester && !isTarget) {
            throw new IllegalArgumentException("Only trade parties can respond");
        }
        if (isRequester && accept) {
            throw new IllegalArgumentException("Requester cannot accept their own trade");
        }

        FantasyParticipant requester = fantasyParticipantRepository.findByFantasyGameSeqAndPlayerId(tx.getFantasyGameSeq(), tx.getRequesterId()).orElse(null);
        FantasyParticipant target = fantasyParticipantRepository.findByFantasyGameSeqAndPlayerId(tx.getFantasyGameSeq(), tx.getTargetId()).orElse(null);
        String requesterTeam = requester != null ? requester.getTeamName() : "Unknown";
        String targetTeam = target != null ? target.getTeamName() : "Unknown";

        if (accept) {
            tx.setStatus(RosterTransaction.TransactionStatus.REQUESTED);
            rosterTransactionRepository.save(tx);

            logAction(tx.getFantasyGameSeq(), responderId, null, RosterLog.LogActionType.TRADE_REQ,
                    "Trade Accepted by " + targetTeam + " - voting started");
            fcmService.sendTopicMessage("game_" + tx.getFantasyGameSeq(), "트레이드 투표 시작",
                    String.format("%s ↔ %s 트레이드가 수락되었습니다. 참가자 투표를 진행해주세요.", requesterTeam, targetTeam));
        } else {
            // 거절(target) 또는 취소(requester) — 공통 처리
            resetGivingPlayersPending(tx);
            tx.setStatus(RosterTransaction.TransactionStatus.REJECTED);
            rosterTransactionRepository.save(tx);

            if (!isRequester) {
                fcmService.sendTopicMessage("game_" + tx.getFantasyGameSeq(), "트레이드 거절",
                        String.format("%s팀이 %s팀의 트레이드 제안을 거절했습니다.", targetTeam, requesterTeam));
            }
        }
    }

    private void resetGivingPlayersPending(RosterTransaction tx) {
        List<Long> allSeqs = getSeqsStream(tx.getGivingPlayerSeqs(), "resetPending giving tx:" + tx.getSeq())
                .collect(Collectors.toList());
        List<Long> receivingSeqs = getSeqsStream(tx.getReceivingPlayerSeqs(), "resetPending receiving tx:" + tx.getSeq())
                .collect(Collectors.toList());
        allSeqs.addAll(receivingSeqs);

        List<DraftPick> picks = draftPickRepository.findByFantasyGameSeq(tx.getFantasyGameSeq()).stream()
                .filter(p -> allSeqs.contains(p.getFantasyPlayerSeq()))
                .collect(Collectors.toList());
        for (DraftPick p : picks) {
            p.setPickStatus(DraftPick.PickStatus.NORMAL);
        }
        draftPickRepository.saveAll(picks);
    }

    private List<DraftPick> validateTradePlayers(Long gameSeq, Long ownerId, List<Long> playerSeqs) {
        List<DraftPick> picks = draftPickRepository.findByFantasyGameSeqAndPlayerId(gameSeq, ownerId);
        List<DraftPick> targetPicks = picks.stream()
                .filter(p -> playerSeqs.contains(p.getFantasyPlayerSeq()))
                .collect(Collectors.toList());

        if (targetPicks.size() != playerSeqs.size()) {
            throw new IllegalArgumentException("Not all players found in roster for user " + ownerId);
        }

        for (DraftPick p : targetPicks) {
            if (p.getPickStatus() != DraftPick.PickStatus.NORMAL) {
                throw new IllegalStateException("Player " + p.getFantasyPlayerSeq() + " is not in NORMAL status");
            }
        }
        return targetPicks;
    }

    @Transactional
    public void processTrade(Long transactionSeq, String decision) {
        RosterTransaction tx = rosterTransactionRepository.findById(transactionSeq)
                .orElseThrow(() -> new IllegalArgumentException("Transaction not found"));

        if (tx.getType() != RosterTransaction.TransactionType.TRADE) {
            throw new IllegalArgumentException("Not a Trade transaction");
        }
        if (tx.getStatus() != RosterTransaction.TransactionStatus.REQUESTED) {
            throw new IllegalStateException("Transaction is already processed");
        }

        List<Long> givingSeqs = getSeqsStream(tx.getGivingPlayerSeqs(), "processTrade giving tx:" + tx.getSeq())
                .collect(Collectors.toList());
        List<Long> receivingSeqs = getSeqsStream(tx.getReceivingPlayerSeqs(), "processTrade receiving tx:" + tx.getSeq())
                .collect(Collectors.toList());

        List<DraftPick> givingPicks = draftPickRepository.findByFantasyGameSeq(tx.getFantasyGameSeq()).stream()
                .filter(p -> givingSeqs.contains(p.getFantasyPlayerSeq()))
                .collect(Collectors.toList());

        if (givingPicks.size() != givingSeqs.size()) {
             throw new IllegalStateException("Some giving players not found");
        }

        List<DraftPick> receivingPicks = draftPickRepository.findByFantasyGameSeq(tx.getFantasyGameSeq()).stream()
                .filter(p -> receivingSeqs.contains(p.getFantasyPlayerSeq()))
                .collect(Collectors.toList());

        if (receivingPicks.size() != receivingSeqs.size()) {
             throw new IllegalStateException("Some receiving players not found");
        }

        if ("APPROVE".equalsIgnoreCase(decision)) {
            // Re-validate ownership for receiving side (giving side is locked/pending)
            for (DraftPick p : receivingPicks) {
                if (!p.getPlayerId().equals(tx.getTargetId())) {
                     throw new IllegalStateException("Player " + p.getFantasyPlayerSeq() + " ownership changed");
                }
                if (p.getPickStatus() != DraftPick.PickStatus.NORMAL) {
                    throw new IllegalStateException("Player " + p.getFantasyPlayerSeq() + " is not NORMAL");
                }
            }
            // Giving picks should be TRADE_PENDING and owned by requester
            for (DraftPick p : givingPicks) {
                if (!p.getPlayerId().equals(tx.getRequesterId())) {
                     throw new IllegalStateException("Giving Player ownership mismatch");
                }
                if (p.getPickStatus() != DraftPick.PickStatus.TRADE_PENDING) {
                     // Should be PENDING
                     // If it's NORMAL, something is wrong.
                     throw new IllegalStateException("Giving Player status mismatch");
                }
            }

            // Check Salary Cap
            validateTradeSalaryCap(tx.getFantasyGameSeq(), tx.getRequesterId(), tx.getTargetId(), givingSeqs, receivingSeqs);

            // Swap
            for (DraftPick p : givingPicks) {
                p.setPlayerId(tx.getTargetId());
                p.setPickStatus(DraftPick.PickStatus.NORMAL);
                p.setAssignedPosition("BENCH");
            }
            for (DraftPick p : receivingPicks) {
                p.setPlayerId(tx.getRequesterId());
                p.setPickStatus(DraftPick.PickStatus.NORMAL);
                p.setAssignedPosition("BENCH");
            }
            draftPickRepository.saveAll(givingPicks);
            draftPickRepository.saveAll(receivingPicks);

            tx.setStatus(RosterTransaction.TransactionStatus.APPROVED);
            String givingNames = fantasyPlayerRepository.findAllById(givingSeqs).stream().map(FantasyPlayer::getName).collect(Collectors.joining(", "));
            String receivingNames = fantasyPlayerRepository.findAllById(receivingSeqs).stream().map(FantasyPlayer::getName).collect(Collectors.joining(", "));
            logAction(tx.getFantasyGameSeq(), tx.getRequesterId(), null, RosterLog.LogActionType.TRADE_SUCCESS, "Trade Approved (Swapped " + givingNames + " <-> " + receivingNames + ")");

            FantasyParticipant requesterInfo = fantasyParticipantRepository.findByFantasyGameSeqAndPlayerId(tx.getFantasyGameSeq(), tx.getRequesterId()).orElse(null);
            FantasyParticipant targetInfo = fantasyParticipantRepository.findByFantasyGameSeqAndPlayerId(tx.getFantasyGameSeq(), tx.getTargetId()).orElse(null);
            String reqTeam = requesterInfo != null ? requesterInfo.getTeamName() : "Unknown";
            String tgtTeam = targetInfo != null ? targetInfo.getTeamName() : "Unknown";
            fcmService.sendTopicMessage("game_" + tx.getFantasyGameSeq(), "트레이드 승인",
                    String.format("%s ↔ %s 트레이드가 승인되었습니다.\n(%s ↔ %s)", reqTeam, tgtTeam, givingNames, receivingNames));

        } else if ("REJECT".equalsIgnoreCase(decision)) {
            for (DraftPick p : givingPicks) {
                p.setPickStatus(DraftPick.PickStatus.NORMAL);
            }
            for (DraftPick p : receivingPicks) {
                p.setPickStatus(DraftPick.PickStatus.NORMAL);
            }
            draftPickRepository.saveAll(givingPicks);
            draftPickRepository.saveAll(receivingPicks);

            tx.setStatus(RosterTransaction.TransactionStatus.REJECTED);
            logAction(tx.getFantasyGameSeq(), tx.getRequesterId(), null, RosterLog.LogActionType.TRADE_REJECT, "Trade Rejected");

            String givingNamesRej = fantasyPlayerRepository.findAllById(givingSeqs).stream().map(FantasyPlayer::getName).collect(Collectors.joining(", "));
            FantasyParticipant requesterInfoRej = fantasyParticipantRepository.findByFantasyGameSeqAndPlayerId(tx.getFantasyGameSeq(), tx.getRequesterId()).orElse(null);
            String reqTeamRej = requesterInfoRej != null ? requesterInfoRej.getTeamName() : "Unknown";
            fcmService.sendTopicMessage("game_" + tx.getFantasyGameSeq(), "트레이드 기각",
                    String.format("%s팀의 트레이드 신청이 기각되었습니다. (주는 선수: %s)", reqTeamRej, givingNamesRej));
        } else {
            throw new IllegalArgumentException("Invalid decision");
        }

        rosterTransactionRepository.save(tx);
    }

    // --- Trade Board Logic ---

    @Transactional(readOnly = true)
    public List<TradeBoardDto> getTradeBoardList(Long gameSeq, Long viewerPlayerId) {
        List<RosterTransaction> trades = new ArrayList<>();

        // 투표 중인 트레이드는 전체 공개
        trades.addAll(rosterTransactionRepository.findByFantasyGameSeqAndStatusAndType(
                gameSeq, RosterTransaction.TransactionStatus.REQUESTED, RosterTransaction.TransactionType.TRADE));

        // SUGGESTED 트레이드는 당사자(신청자, 상대방)에게만 노출
        if (viewerPlayerId != null) {
            rosterTransactionRepository.findByFantasyGameSeqAndStatusAndType(
                            gameSeq, RosterTransaction.TransactionStatus.SUGGESTED, RosterTransaction.TransactionType.TRADE)
                    .stream()
                    .filter(tx -> tx.getRequesterId().equals(viewerPlayerId) || tx.getTargetId().equals(viewerPlayerId))
                    .forEach(trades::add);
        }

        if (trades.isEmpty()) {
            return new ArrayList<>();
        }

        List<Long> tradeSeqs = trades.stream().map(RosterTransaction::getSeq).collect(Collectors.toList());

        Set<Long> allParticipantIds = trades.stream()
                .flatMap(tx -> java.util.stream.Stream.of(tx.getRequesterId(), tx.getTargetId()))
                .collect(Collectors.toSet());
        Map<Long, String> teamNameMap = fantasyParticipantRepository.findByFantasyGameSeqAndPlayerIdIn(gameSeq, allParticipantIds)
                .stream().collect(Collectors.toMap(FantasyParticipant::getPlayerId, FantasyParticipant::getTeamName, (a, b) -> a));

        Set<Long> allPlayerSeqs = trades.stream()
                .flatMap(tx -> java.util.stream.Stream.concat(
                        getSeqsStream(tx.getGivingPlayerSeqs(), "tradeBoard giving tx:" + tx.getSeq()),
                        getSeqsStream(tx.getReceivingPlayerSeqs(), "tradeBoard receiving tx:" + tx.getSeq())))
                .collect(Collectors.toSet());
        Map<Long, FantasyPlayer> playerMap = fantasyPlayerRepository.findAllById(allPlayerSeqs)
                .stream().collect(Collectors.toMap(FantasyPlayer::getSeq, p -> p));

        Map<Long, List<TradeBoard>> votesMap = tradeBoardRepository.findByTradeSeqIn(tradeSeqs)
                .stream().collect(Collectors.groupingBy(TradeBoard::getTradeSeq));

        return trades.stream().map(tx -> {
            List<TradeBoard> votes = votesMap.getOrDefault(tx.getSeq(), new ArrayList<>());
            int agreeCount = (int) votes.stream().filter(TradeBoard::getVoteAgree).count();
            int disagreeCount = (int) votes.stream().filter(v -> !v.getVoteAgree()).count();
            boolean isParty = viewerPlayerId != null &&
                    (tx.getRequesterId().equals(viewerPlayerId) || tx.getTargetId().equals(viewerPlayerId));
            boolean canRespond = viewerPlayerId != null
                    && tx.getStatus() == RosterTransaction.TransactionStatus.SUGGESTED
                    && tx.getTargetId().equals(viewerPlayerId);
            Boolean myVote = null;
            if (viewerPlayerId != null && !isParty) {
                myVote = votes.stream()
                        .filter(v -> v.getPlayerId().equals(viewerPlayerId))
                        .map(TradeBoard::getVoteAgree)
                        .findFirst()
                        .orElse(null);
            }

            List<TradeBoardDto.PlayerInfo> givingPlayers = getSeqsStream(tx.getGivingPlayerSeqs(), "tradeBoard tx:" + tx.getSeq())
                    .map(playerMap::get)
                    .filter(java.util.Objects::nonNull)
                    .map(p -> TradeBoardDto.PlayerInfo.builder().playerName(p.getName()).teamName(p.getTeam()).build())
                    .collect(Collectors.toList());

            List<TradeBoardDto.PlayerInfo> receivingPlayers = getSeqsStream(tx.getReceivingPlayerSeqs(), "tradeBoard tx:" + tx.getSeq())
                    .map(playerMap::get)
                    .filter(java.util.Objects::nonNull)
                    .map(p -> TradeBoardDto.PlayerInfo.builder().playerName(p.getName()).teamName(p.getTeam()).build())
                    .collect(Collectors.toList());

            return TradeBoardDto.builder()
                    .transactionSeq(tx.getSeq())
                    .status(tx.getStatus().name())
                    .comment(tx.getComment())
                    .requesterTeamName(teamNameMap.getOrDefault(tx.getRequesterId(), "Unknown"))
                    .targetTeamName(teamNameMap.getOrDefault(tx.getTargetId(), "Unknown"))
                    .givingPlayers(givingPlayers)
                    .receivingPlayers(receivingPlayers)
                    .agreeCount(agreeCount)
                    .disagreeCount(disagreeCount)
                    .myVote(myVote)
                    .isParty(isParty)
                    .canRespond(canRespond)
                    .createdAt(tx.getCreatedAt())
                    .build();
        }).collect(Collectors.toList());
    }

    @Transactional
    public void voteOnTrade(Long gameSeq, Long transactionSeq, Long voterId, boolean voteAgree) {
        RosterTransaction tx = rosterTransactionRepository.findById(transactionSeq)
                .orElseThrow(() -> new IllegalArgumentException("Transaction not found"));

        if (tx.getType() != RosterTransaction.TransactionType.TRADE) {
            throw new IllegalArgumentException("Not a Trade transaction");
        }
        if (tx.getStatus() != RosterTransaction.TransactionStatus.REQUESTED) {
            throw new IllegalStateException("Trade is already processed");
        }
        if (!tx.getFantasyGameSeq().equals(gameSeq)) {
            throw new IllegalArgumentException("Transaction does not belong to this game");
        }
        if (tx.getRequesterId().equals(voterId) || tx.getTargetId().equals(voterId)) {
            throw new IllegalArgumentException("Trade parties cannot vote");
        }

        fantasyParticipantRepository.findByFantasyGameSeqAndPlayerId(gameSeq, voterId)
                .orElseThrow(() -> new IllegalArgumentException("Not a participant of this game"));

        Optional<TradeBoard> existingVote = tradeBoardRepository.findByTradeSeqAndPlayerId(transactionSeq, voterId);
        if (existingVote.isPresent()) {
            TradeBoard vote = existingVote.get();
            if (vote.getVotedAt().isAfter(LocalDateTime.now().minusMinutes(1))) {
                throw new IllegalStateException("재투표는 이전 투표로부터 1분 후에 가능합니다");
            }
            vote.setVoteAgree(voteAgree);
            vote.setVotedAt(LocalDateTime.now());
            tradeBoardRepository.save(vote);
        } else {
            tradeBoardRepository.save(TradeBoard.builder()
                    .tradeSeq(transactionSeq)
                    .playerId(voterId)
                    .voteAgree(voteAgree)
                    .votedAt(LocalDateTime.now())
                    .build());
        }

        List<FantasyParticipant> allParticipants = fantasyParticipantRepository.findByFantasyGameSeq(gameSeq);
        int n = allParticipants.size();
        List<TradeBoard> votes = tradeBoardRepository.findByTradeSeq(transactionSeq);
        long agreeCnt = votes.stream().filter(TradeBoard::getVoteAgree).count();
        long disagreeCnt = votes.stream().filter(v -> !v.getVoteAgree()).count();

        int rejectThreshold = (int) Math.ceil((n - 2) / 2.0);
        int approveThreshold = (n - 2) / 2 + 1;

        if (disagreeCnt >= rejectThreshold) {
            processTrade(transactionSeq, "REJECT");
        } else if (agreeCnt >= approveThreshold) {
            processTrade(transactionSeq, "APPROVE");
        }
    }

    private void validateTradeSalaryCap(Long gameSeq, Long requesterId, Long targetId, List<Long> givingSeqs, List<Long> receivingSeqs) {
        FantasyGame game = fantasyGameRepository.findById(gameSeq).orElseThrow();
        if (game.getSalaryCap() == null || game.getSalaryCap() <= 0) return;

        List<DraftPick> requesterPicks = draftPickRepository.findByFantasyGameSeqAndPlayerId(gameSeq, requesterId);
        List<DraftPick> targetPicks = draftPickRepository.findByFantasyGameSeqAndPlayerId(gameSeq, targetId);

        // Calculate Requester New Cost
        FantasyParticipant requesterParticipant = fantasyParticipantRepository.findByFantasyGameSeqAndPlayerId(gameSeq, requesterId).orElse(null);
        int requesterCost = calculateTeamCostAfterTrade(game, requesterParticipant, requesterPicks, givingSeqs, receivingSeqs);
        if (requesterCost > game.getSalaryCap()) {
            throw new IllegalStateException("Trade failed: Requester Salary Cap Exceeded (" + requesterCost + " > " + game.getSalaryCap() + ")");
        }

        // Calculate Target New Cost
        // For target: Giving = receivingSeqs, Receiving = givingSeqs
        FantasyParticipant targetParticipant = fantasyParticipantRepository.findByFantasyGameSeqAndPlayerId(gameSeq, targetId).orElse(null);
        int targetCost = calculateTeamCostAfterTrade(game, targetParticipant, targetPicks, receivingSeqs, givingSeqs);
        if (targetCost > game.getSalaryCap()) {
            throw new IllegalStateException("Trade failed: Target Salary Cap Exceeded (" + targetCost + " > " + game.getSalaryCap() + ")");
        }
    }

    private int calculateTeamCostAfterTrade(FantasyGame game, FantasyParticipant participant, List<DraftPick> currentPicks, List<Long> removingSeqs, List<Long> addingSeqs) {
        Set<Long> teamSeqs = currentPicks.stream().map(DraftPick::getFantasyPlayerSeq).collect(Collectors.toSet());
        teamSeqs.removeAll(removingSeqs);
        teamSeqs.addAll(addingSeqs);

        if (teamSeqs.isEmpty()) return 0;
        return com.sayai.record.fantasy.util.SalaryCapCalculator.calculateTeamCost(game, participant, fantasyPlayerRepository.findAllById(teamSeqs)).getTotalCost();
    }

    private void logAction(Long gameSeq, Long participantId, Long playerSeq, RosterLog.LogActionType type, String details) {
        RosterLog entry = RosterLog.builder()
                .fantasyGameSeq(gameSeq)
                .participantId(participantId)
                .fantasyPlayerSeq(playerSeq)
                .actionType(type)
                .details(details)
                .build();
        rosterLogRepository.save(entry);
    }

    @Transactional(readOnly = true)
    public List<RosterTransaction> getTransactions(Long gameSeq, String status) {
        if (status == null || status.isEmpty()) {
            // Find all for game? Or specific status? Let's assume requested status if not specified, or all.
            // Requirement implies admin views list to process.
            return rosterTransactionRepository.findByFantasyGameSeqAndStatus(gameSeq, RosterTransaction.TransactionStatus.REQUESTED);
        }
        try {
            RosterTransaction.TransactionStatus ts = RosterTransaction.TransactionStatus.valueOf(status);
            return rosterTransactionRepository.findByFantasyGameSeqAndStatus(gameSeq, ts);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid status: " + status);
        }
    }

    public List<AdminRosterTransactionDto> getAdminTransactions(Long gameSeq, String status) {
        List<RosterTransaction> transactions = getTransactions(gameSeq, status);

        // Collect all distinct player seqs
        Set<Long> allPlayerSeqs = transactions.stream()
                .flatMap(tx -> {
                    java.util.stream.Stream<Long> giving = getSeqsStream(tx.getGivingPlayerSeqs(), "admin preload giving tx:" + tx.getSeq());
                    java.util.stream.Stream<Long> receiving = getSeqsStream(tx.getReceivingPlayerSeqs(), "admin preload receiving tx:" + tx.getSeq());
                    return java.util.stream.Stream.concat(giving, receiving);
                })
                .collect(Collectors.toSet());

        // Batch fetch all players
        java.util.Map<Long, FantasyPlayer> playerMap = fantasyPlayerRepository.findAllById(allPlayerSeqs)
                .stream()
                .collect(Collectors.toMap(FantasyPlayer::getSeq, p -> p));

        return transactions.stream().map(tx -> {
            String givingDetails = getPlayerDetailsMap(tx.getGivingPlayerSeqs(), playerMap, "admin tx:" + tx.getSeq());
            String receivingDetails = getPlayerDetailsMap(tx.getReceivingPlayerSeqs(), playerMap, "admin tx:" + tx.getSeq());
            return AdminRosterTransactionDto.from(tx, givingDetails, receivingDetails);
        }).collect(Collectors.toList());
    }

    private java.util.stream.Stream<Long> getSeqsStream(String playerSeqs, String context) {
        if (playerSeqs == null || playerSeqs.isEmpty()) {
            return java.util.stream.Stream.empty();
        }
        return Arrays.stream(playerSeqs.split(","))
                .map(String::trim)
                .flatMap(s -> {
                    try {
                        return java.util.stream.Stream.of(Long.valueOf(s));
                    } catch (NumberFormatException e) {
                        log.error("Failed to parse player sequence: '{}' (Context: {})", s, context);
                        return java.util.stream.Stream.empty();
                    }
                });
    }

    private String getPlayerDetailsMap(String playerSeqs, java.util.Map<Long, FantasyPlayer> playerMap, String context) {
        if (playerSeqs == null || playerSeqs.isEmpty()) {
            return null;
        }
        return getSeqsStream(playerSeqs, context)
                .map(playerMap::get)
                .filter(java.util.Objects::nonNull)
                .map(p -> p.getName() + " (" + p.getTeam() + ") - " + p.getPosition())
                .collect(Collectors.joining(", "));
    }

    @Transactional
    public void processTransaction(Long transactionSeq, String decision, Long targetParticipantId) {
        RosterTransaction tx = rosterTransactionRepository.findById(transactionSeq)
                .orElseThrow(() -> new IllegalArgumentException("Transaction not found"));

        if (tx.getType() == RosterTransaction.TransactionType.WAIVER) {
            processWaiver(transactionSeq, decision, targetParticipantId);
        } else if (tx.getType() == RosterTransaction.TransactionType.TRADE) {
            processTrade(transactionSeq, decision);
        } else {
            throw new IllegalArgumentException("Unknown transaction type");
        }
    }
}
