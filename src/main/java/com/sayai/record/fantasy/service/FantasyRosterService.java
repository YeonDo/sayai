package com.sayai.record.fantasy.service;

import com.sayai.record.fantasy.dto.FantasyPlayerDto;
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
        logAction(gameSeq, requesterId, fantasyPlayerSeq, RoasterLog.LogActionType.WAIVER_RELEASE, "Waiver Requested");
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

            logAction(tx.getFantasyGameSeq(), targetParticipantId, playerSeq, RoasterLog.LogActionType.WAIVER_CLAIM, "Claimed by " + targetParticipantId);

        } else if ("FA".equalsIgnoreCase(decision)) {
            // Move to FA (Delete Pick)
            draftPickRepository.delete(pick);

            tx.setStatus(RoasterTransaction.TransactionStatus.FA_MOVED);

            logAction(tx.getFantasyGameSeq(), tx.getRequesterId(), playerSeq, RoasterLog.LogActionType.WAIVER_FA, "Moved to FA");

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

        logAction(gameSeq, requesterId, null, RoasterLog.LogActionType.TRADE_REQ, "Trade Requested with " + targetId);
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
            logAction(tx.getFantasyGameSeq(), tx.getRequesterId(), null, RoasterLog.LogActionType.TRADE_SUCCESS, "Trade Approved");

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

    // --- FA Logic ---

    @Transactional(readOnly = true)
    public List<FantasyPlayerDto> getFAList(Long gameSeq) {
        List<DraftPick> picks = draftPickRepository.findByFantasyGameSeq(gameSeq);
        java.util.Set<Long> pickedSeqs = picks.stream()
                .map(DraftPick::getFantasyPlayerSeq)
                .collect(Collectors.toSet());

        List<FantasyPlayer> available;
        if (pickedSeqs.isEmpty()) {
            available = fantasyPlayerRepository.findAll();
        } else {
            available = fantasyPlayerRepository.findBySeqNotIn(pickedSeqs);
        }

        return available.stream()
                .map(FantasyPlayerDto::from)
                .collect(Collectors.toList());
    }

    @Transactional
    public void signFA(Long gameSeq, Long participantId, Long fantasyPlayerSeq) {
        FantasyGame game = fantasyGameRepository.findById(gameSeq)
                .orElseThrow(() -> new IllegalArgumentException("Game not found"));

        if (game.getStatus() != FantasyGame.GameStatus.ONGOING) {
             throw new IllegalStateException("Game is not ongoing");
        }

        boolean exists = draftPickRepository.existsByFantasyGameSeqAndFantasyPlayerSeq(gameSeq, fantasyPlayerSeq);
        if (exists) {
            throw new IllegalStateException("Player already picked");
        }

        FantasyPlayer player = fantasyPlayerRepository.findById(fantasyPlayerSeq)
                .orElseThrow(() -> new IllegalArgumentException("Player not found"));

        List<DraftPick> myPicks = draftPickRepository.findByFantasyGameSeqAndPlayerId(gameSeq, participantId);

        // Check Roster Size
        int limit = (game.getRuleType() == FantasyGame.RuleType.RULE_2) ? 21 : 20;
        if (myPicks.size() >= limit) {
             throw new IllegalStateException("Roster full (Max " + limit + ")");
        }

        // Check Cost
        if (game.getSalaryCap() != null && game.getSalaryCap() > 0) {
            int currentCost = 0;
            List<Long> myPlayerSeqs = myPicks.stream().map(DraftPick::getFantasyPlayerSeq).collect(Collectors.toList());
            if (!myPlayerSeqs.isEmpty()) {
                currentCost = fantasyPlayerRepository.findAllById(myPlayerSeqs).stream()
                        .mapToInt(p -> p.getCost() == null ? 0 : p.getCost())
                        .sum();
            }
            int newCost = player.getCost() == null ? 0 : player.getCost();
            if (currentCost + newCost > game.getSalaryCap()) {
                throw new IllegalStateException("Salary Cap exceeded");
            }
        }

        long count = draftPickRepository.countByFantasyGameSeq(gameSeq);

        DraftPick pick = DraftPick.builder()
                .fantasyGameSeq(gameSeq)
                .playerId(participantId)
                .fantasyPlayerSeq(fantasyPlayerSeq)
                .pickNumber((int) count + 1)
                .assignedPosition("BENCH")
                .pickStatus(DraftPick.PickStatus.NORMAL)
                .build();

        draftPickRepository.save(pick);

        logAction(gameSeq, participantId, fantasyPlayerSeq, RoasterLog.LogActionType.FA_ADD, "Signed FA");
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
