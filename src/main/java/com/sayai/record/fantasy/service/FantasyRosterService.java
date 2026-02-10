package com.sayai.record.fantasy.service;

import com.sayai.record.fantasy.entity.*;
import com.sayai.record.fantasy.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class FantasyRosterService {

    private final DraftPickRepository draftPickRepository;
    private final FantasyGameRepository fantasyGameRepository;
    private final RoasterTransactionRepository roasterTransactionRepository;
    private final RoasterLogRepository roasterLogRepository;
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
        RoasterTransaction tx = RoasterTransaction.builder()
                .fantasyGameSeq(gameSeq)
                .requesterId(requesterId)
                .type(RoasterTransaction.TransactionType.WAIVER)
                .status(RoasterTransaction.TransactionStatus.REQUESTED)
                .givingPlayerSeqs(String.valueOf(fantasyPlayerSeq))
                .build();
        roasterTransactionRepository.save(tx);

        // Log
        FantasyPlayer player = fantasyPlayerRepository.findById(fantasyPlayerSeq).orElseThrow();
        logAction(gameSeq, requesterId, fantasyPlayerSeq, RoasterLog.LogActionType.WAIVER_RELEASE, player.getName() + " - Waiver Requested");
    }

    @Transactional
    public void processWaiver(Long transactionSeq, String decision, Long targetParticipantId) {
        RoasterTransaction tx = roasterTransactionRepository.findById(transactionSeq)
                .orElseThrow(() -> new IllegalArgumentException("Transaction not found"));

        if (tx.getType() != RoasterTransaction.TransactionType.WAIVER) {
            throw new IllegalArgumentException("Not a Waiver transaction");
        }
        if (tx.getStatus() != RoasterTransaction.TransactionStatus.REQUESTED) {
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

            tx.setStatus(RoasterTransaction.TransactionStatus.APPROVED);
            tx.setTargetId(targetParticipantId);

            logAction(tx.getFantasyGameSeq(), targetParticipantId, playerSeq, RoasterLog.LogActionType.WAIVER_CLAIM, player.getName() + " - Claimed by " + targetParticipantId);

        } else if ("FA".equalsIgnoreCase(decision)) {
            // Move to FA (Delete Pick)
            draftPickRepository.delete(pick);

            tx.setStatus(RoasterTransaction.TransactionStatus.FA_MOVED);

            logAction(tx.getFantasyGameSeq(), tx.getRequesterId(), playerSeq, RoasterLog.LogActionType.WAIVER_FA, player.getName() + " - Moved to FA");

        } else {
            throw new IllegalArgumentException("Invalid decision: " + decision);
        }

        roasterTransactionRepository.save(tx);
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
        RoasterTransaction tx = RoasterTransaction.builder()
                .fantasyGameSeq(gameSeq)
                .requesterId(requesterId)
                .targetId(targetId)
                .type(RoasterTransaction.TransactionType.TRADE)
                .status(RoasterTransaction.TransactionStatus.REQUESTED)
                .givingPlayerSeqs(givingPlayerSeqs.stream().map(String::valueOf).collect(Collectors.joining(",")))
                .receivingPlayerSeqs(receivingPlayerSeqs.stream().map(String::valueOf).collect(Collectors.joining(",")))
                .build();
        roasterTransactionRepository.save(tx);

        String givingNames = fantasyPlayerRepository.findAllById(givingPlayerSeqs).stream().map(FantasyPlayer::getName).collect(Collectors.joining(", "));
        String receivingNames = fantasyPlayerRepository.findAllById(receivingPlayerSeqs).stream().map(FantasyPlayer::getName).collect(Collectors.joining(", "));

        logAction(gameSeq, requesterId, null, RoasterLog.LogActionType.TRADE_REQ, "Trade Requested with " + targetId + " (Give: " + givingNames + " / Get: " + receivingNames + ")");
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
        RoasterTransaction tx = roasterTransactionRepository.findById(transactionSeq)
                .orElseThrow(() -> new IllegalArgumentException("Transaction not found"));

        if (tx.getType() != RoasterTransaction.TransactionType.TRADE) {
            throw new IllegalArgumentException("Not a Trade transaction");
        }
        if (tx.getStatus() != RoasterTransaction.TransactionStatus.REQUESTED) {
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

            tx.setStatus(RoasterTransaction.TransactionStatus.APPROVED);
            String givingNames = fantasyPlayerRepository.findAllById(givingSeqs).stream().map(FantasyPlayer::getName).collect(Collectors.joining(", "));
            String receivingNames = fantasyPlayerRepository.findAllById(receivingSeqs).stream().map(FantasyPlayer::getName).collect(Collectors.joining(", "));
            logAction(tx.getFantasyGameSeq(), tx.getRequesterId(), null, RoasterLog.LogActionType.TRADE_SUCCESS, "Trade Approved (Swapped " + givingNames + " <-> " + receivingNames + ")");

        } else if ("REJECT".equalsIgnoreCase(decision)) {
            // Revert Giving status
            for (DraftPick p : givingPicks) {
                p.setPickStatus(DraftPick.PickStatus.NORMAL);
            }
            draftPickRepository.saveAll(givingPicks);

            tx.setStatus(RoasterTransaction.TransactionStatus.REJECTED);
            logAction(tx.getFantasyGameSeq(), tx.getRequesterId(), null, RoasterLog.LogActionType.TRADE_REJECT, "Trade Rejected");
        } else {
            throw new IllegalArgumentException("Invalid decision");
        }

        roasterTransactionRepository.save(tx);
    }

    private void logAction(Long gameSeq, Long participantId, Long playerSeq, RoasterLog.LogActionType type, String details) {
        RoasterLog entry = RoasterLog.builder()
                .fantasyGameSeq(gameSeq)
                .participantId(participantId)
                .fantasyPlayerSeq(playerSeq)
                .actionType(type)
                .details(details)
                .build();
        roasterLogRepository.save(entry);
    }

    @Transactional(readOnly = true)
    public List<RoasterTransaction> getTransactions(Long gameSeq, String status) {
        if (status == null || status.isEmpty()) {
            // Find all for game? Or specific status? Let's assume requested status if not specified, or all.
            // Requirement implies admin views list to process.
            return roasterTransactionRepository.findByFantasyGameSeqAndStatus(gameSeq, RoasterTransaction.TransactionStatus.REQUESTED);
        }
        try {
            RoasterTransaction.TransactionStatus ts = RoasterTransaction.TransactionStatus.valueOf(status);
            return roasterTransactionRepository.findByFantasyGameSeqAndStatus(gameSeq, ts);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid status: " + status);
        }
    }

    @Transactional
    public void processTransaction(Long transactionSeq, String decision, Long targetParticipantId) {
        RoasterTransaction tx = roasterTransactionRepository.findById(transactionSeq)
                .orElseThrow(() -> new IllegalArgumentException("Transaction not found"));

        if (tx.getType() == RoasterTransaction.TransactionType.WAIVER) {
            processWaiver(transactionSeq, decision, targetParticipantId);
        } else if (tx.getType() == RoasterTransaction.TransactionType.TRADE) {
            processTrade(transactionSeq, decision);
        } else {
            throw new IllegalArgumentException("Unknown transaction type");
        }
    }
}
