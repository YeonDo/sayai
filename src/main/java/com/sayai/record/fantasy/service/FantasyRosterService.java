package com.sayai.record.fantasy.service;

import com.sayai.record.fantasy.entity.*;
import com.sayai.record.fantasy.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.List;
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

    // --- Waiver Logic ---

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

        // Update Pick Status
        pick.setPickStatus(DraftPick.PickStatus.WAIVER_REQ);
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

        Long playerSeq = Long.parseLong(tx.getGivingPlayerSeqs());
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

            logAction(tx.getFantasyGameSeq(), targetParticipantId, playerSeq, RosterLog.LogActionType.WAIVER_CLAIM, player.getName() + " - Claimed by " + targetParticipantId);

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
    public void requestTrade(Long gameSeq, Long requesterId, Long targetId, List<Long> givingPlayerSeqs, List<Long> receivingPlayerSeqs) {
        // Validation
        if (givingPlayerSeqs == null || givingPlayerSeqs.isEmpty()) {
            throw new IllegalArgumentException("Must give at least one player");
        }
        if (receivingPlayerSeqs == null || receivingPlayerSeqs.isEmpty()) {
            throw new IllegalArgumentException("Must receive at least one player");
        }
        if (givingPlayerSeqs.size() > 2 || receivingPlayerSeqs.size() > 2) {
            throw new IllegalArgumentException("Max 2 players per side");
        }

        // Validate Giving Players (Ownership + Bench + Normal Status)
        List<DraftPick> givingPicks = validateTradePlayers(gameSeq, requesterId, givingPlayerSeqs);
        // Validate Receiving Players (Ownership + Bench + Normal Status)
        validateTradePlayers(gameSeq, targetId, receivingPlayerSeqs);

        // Mark Giving Players as Pending
        for (DraftPick p : givingPicks) {
            p.setPickStatus(DraftPick.PickStatus.TRADE_PENDING);
        }
        draftPickRepository.saveAll(givingPicks);

        // Create Transaction
        RosterTransaction tx = RosterTransaction.builder()
                .fantasyGameSeq(gameSeq)
                .requesterId(requesterId)
                .targetId(targetId)
                .type(RosterTransaction.TransactionType.TRADE)
                .status(RosterTransaction.TransactionStatus.REQUESTED)
                .givingPlayerSeqs(givingPlayerSeqs.stream().map(String::valueOf).collect(Collectors.joining(",")))
                .receivingPlayerSeqs(receivingPlayerSeqs.stream().map(String::valueOf).collect(Collectors.joining(",")))
                .build();
        rosterTransactionRepository.save(tx);

        String givingNames = fantasyPlayerRepository.findAllById(givingPlayerSeqs).stream().map(FantasyPlayer::getName).collect(Collectors.joining(", "));
        String receivingNames = fantasyPlayerRepository.findAllById(receivingPlayerSeqs).stream().map(FantasyPlayer::getName).collect(Collectors.joining(", "));

        logAction(gameSeq, requesterId, null, RosterLog.LogActionType.TRADE_REQ, "Trade Requested with " + targetId + " (Give: " + givingNames + " / Get: " + receivingNames + ")");
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
            if (p.getAssignedPosition() != null && !p.getAssignedPosition().contains("BENCH")) {
                 throw new IllegalStateException("Player " + p.getFantasyPlayerSeq() + " must be on BENCH");
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

        List<Long> givingSeqs = Arrays.stream(tx.getGivingPlayerSeqs().split(","))
                .map(Long::valueOf).collect(Collectors.toList());
        List<Long> receivingSeqs = Arrays.stream(tx.getReceivingPlayerSeqs().split(","))
                .map(Long::valueOf).collect(Collectors.toList());

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
                if (p.getAssignedPosition() != null && !p.getAssignedPosition().contains("BENCH")) {
                    throw new IllegalStateException("Target player " + p.getFantasyPlayerSeq() + " is no longer on BENCH");
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

        } else if ("REJECT".equalsIgnoreCase(decision)) {
            // Revert Giving status
            for (DraftPick p : givingPicks) {
                p.setPickStatus(DraftPick.PickStatus.NORMAL);
            }
            draftPickRepository.saveAll(givingPicks);

            tx.setStatus(RosterTransaction.TransactionStatus.REJECTED);
            logAction(tx.getFantasyGameSeq(), tx.getRequesterId(), null, RosterLog.LogActionType.TRADE_REJECT, "Trade Rejected");
        } else {
            throw new IllegalArgumentException("Invalid decision");
        }

        rosterTransactionRepository.save(tx);
    }

    private void validateTradeSalaryCap(Long gameSeq, Long requesterId, Long targetId, List<Long> givingSeqs, List<Long> receivingSeqs) {
        FantasyGame game = fantasyGameRepository.findById(gameSeq).orElseThrow();
        if (game.getSalaryCap() == null || game.getSalaryCap() <= 0) return;

        List<DraftPick> requesterPicks = draftPickRepository.findByFantasyGameSeqAndPlayerId(gameSeq, requesterId);
        List<DraftPick> targetPicks = draftPickRepository.findByFantasyGameSeqAndPlayerId(gameSeq, targetId);

        // Calculate Requester New Cost
        int requesterCost = calculateTeamCostAfterTrade(requesterPicks, givingSeqs, receivingSeqs);
        if (requesterCost > game.getSalaryCap()) {
            throw new IllegalStateException("Trade failed: Requester Salary Cap Exceeded (" + requesterCost + " > " + game.getSalaryCap() + ")");
        }

        // Calculate Target New Cost
        // For target: Giving = receivingSeqs, Receiving = givingSeqs
        int targetCost = calculateTeamCostAfterTrade(targetPicks, receivingSeqs, givingSeqs);
        if (targetCost > game.getSalaryCap()) {
            throw new IllegalStateException("Trade failed: Target Salary Cap Exceeded (" + targetCost + " > " + game.getSalaryCap() + ")");
        }
    }

    private int calculateTeamCostAfterTrade(List<DraftPick> currentPicks, List<Long> removingSeqs, List<Long> addingSeqs) {
        Set<Long> teamSeqs = currentPicks.stream().map(DraftPick::getFantasyPlayerSeq).collect(Collectors.toSet());
        teamSeqs.removeAll(removingSeqs);
        teamSeqs.addAll(addingSeqs);

        if (teamSeqs.isEmpty()) return 0;
        return fantasyPlayerRepository.findAllById(teamSeqs).stream()
                .mapToInt(p -> p.getCost() == null ? 0 : p.getCost())
                .sum();
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
