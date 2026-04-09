package com.sayai.record.fantasy.service;

import com.sayai.record.fantasy.entity.FantasyWaiverClaim;
import com.sayai.record.fantasy.entity.FantasyWaiverOrder;
import com.sayai.record.fantasy.entity.RosterTransaction;
import com.sayai.record.fantasy.repository.FantasyWaiverClaimRepository;
import com.sayai.record.fantasy.repository.FantasyWaiverOrderRepository;
import com.sayai.record.fantasy.repository.RosterTransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Slf4j
@Component
@RequiredArgsConstructor
public class WaiverScheduler {

    private final RosterTransactionRepository transactionRepository;
    private final FantasyWaiverClaimRepository waiverClaimRepository;
    private final FantasyWaiverOrderRepository waiverOrderRepository;
    private final FantasyRosterService rosterService;

    // Runs every 30 minutes from 10:00 to 23:30 KST
    @Scheduled(cron = "0 0,30 10-23 * * *", zone = "Asia/Seoul")
    @Transactional
    public void processWaivers() {
        log.info("Starting scheduled waiver processing...");

        LocalDateTime cutoffTime = LocalDateTime.now().minusMinutes(30);

        List<RosterTransaction> pendingWaivers = transactionRepository.findByStatusAndType(
                RosterTransaction.TransactionStatus.REQUESTED,
                RosterTransaction.TransactionType.WAIVER);

        // Sort by seq to process older waivers first
        pendingWaivers.sort(java.util.Comparator.comparing(RosterTransaction::getSeq));

        int processed = 0;
        java.util.Map<Long, Integer> maxOrdersByGameSeq = new java.util.HashMap<>();

        for (RosterTransaction tx : pendingWaivers) {
            // Only process if the waiver was requested at least 30 minutes ago
            if (tx.getCreatedAt() != null && tx.getCreatedAt().isBefore(cutoffTime)) {

                List<FantasyWaiverClaim> claims = waiverClaimRepository.findByWaiverSeq(tx.getSeq());

                if (!claims.isEmpty()) {
                    Long winnerId = null;
                    int minOrder = Integer.MAX_VALUE;

                    for (FantasyWaiverClaim claim : claims) {
                        Long claimerId = claim.getClaimPlayerId();
                        Optional<FantasyWaiverOrder> orderOpt = waiverOrderRepository.findByGameSeqAndPlayerId(tx.getFantasyGameSeq(), claimerId);
                        if (orderOpt.isPresent()) {
                            int currentOrder = orderOpt.get().getOrderNum();
                            if (currentOrder < minOrder) {
                                minOrder = currentOrder;
                                winnerId = claimerId;
                            }
                        }
                    }

                    if (winnerId != null) {
                        log.info("Processing waiver tx {} - Claimed by {}", tx.getSeq(), winnerId);

                        try {
                            rosterService.processWaiver(tx.getSeq(), "CLAIM", winnerId);

                            // Update order for the successful claimer
                            FantasyWaiverOrder claimerOrder = waiverOrderRepository.findByGameSeqAndPlayerId(tx.getFantasyGameSeq(), winnerId)
                                    .orElseThrow(() -> new IllegalStateException("Claimer waiver order not found"));

                            Long gameSeq = tx.getFantasyGameSeq();
                            Integer maxOrder = maxOrdersByGameSeq.computeIfAbsent(gameSeq, key -> {
                                Integer dbMax = waiverOrderRepository.findMaxOrderNumByGameSeq(key);
                                return dbMax != null ? dbMax : 0;
                            });

                            int nextOrder = maxOrder + 1;
                            maxOrdersByGameSeq.put(gameSeq, nextOrder); // Update cache for next transactions in same game

                            claimerOrder.setOrderNum(nextOrder);
                            waiverOrderRepository.save(claimerOrder);
                        } catch (Exception e) {
                            log.error("Failed to process waiver claim for tx {}: {}", tx.getSeq(), e.getMessage());
                        }
                    } else {
                        log.info("Processing waiver tx {} - No valid claims found, moving to FA", tx.getSeq());
                        try {
                            rosterService.processWaiver(tx.getSeq(), "FA", null);
                        } catch (Exception e) {
                            log.error("Failed to move waiver to FA for tx {}: {}", tx.getSeq(), e.getMessage());
                        }
                    }
                } else {
                    log.info("Processing waiver tx {} - No claims, moving to FA", tx.getSeq());
                    try {
                        rosterService.processWaiver(tx.getSeq(), "FA", null);
                    } catch (Exception e) {
                        log.error("Failed to move waiver to FA for tx {}: {}", tx.getSeq(), e.getMessage());
                    }
                }
                processed++;
            }
        }

        log.info("Waiver processing completed. Processed {} waivers.", processed);
    }
}
