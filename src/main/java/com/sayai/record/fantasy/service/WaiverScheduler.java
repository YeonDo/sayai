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
    public void processAll() {
        processTrades();
        processWaivers();
    }

    private void processTrades() {
        log.info("Starting scheduled trade processing...");

        LocalDateTime cutoffTime = LocalDateTime.now().minusHours(24);
        int processed = 0;

        // SUGGESTED 트레이드 24시간 내 미수락 → 자동 거절
        List<RosterTransaction> suggestedTrades = transactionRepository.findByStatusAndType(
                RosterTransaction.TransactionStatus.SUGGESTED,
                RosterTransaction.TransactionType.TRADE);
        for (RosterTransaction tx : suggestedTrades) {
            if (tx.getCreatedAt() != null && tx.getCreatedAt().isBefore(cutoffTime)) {
                try {
                    log.info("Auto-rejecting suggested trade tx {} (24h elapsed, no response)", tx.getSeq());
                    rosterService.respondToTrade(tx.getSeq(), tx.getTargetId(), false);
                    processed++;
                } catch (Exception e) {
                    log.error("Failed to auto-reject suggested trade tx {}: {}", tx.getSeq(), e.getMessage());
                }
            }
        }

        // REQUESTED 트레이드 24시간 경과 → 미투표 찬성 간주하여 자동 승인
        List<RosterTransaction> requestedTrades = transactionRepository.findByStatusAndType(
                RosterTransaction.TransactionStatus.REQUESTED,
                RosterTransaction.TransactionType.TRADE);
        for (RosterTransaction tx : requestedTrades) {
            if (tx.getUpdatedAt() != null && tx.getUpdatedAt().isBefore(cutoffTime)) {
                try {
                    log.info("Auto-approving trade tx {} (24h elapsed, unvoted = agree)", tx.getSeq());
                    rosterService.processTrade(tx.getSeq(), "APPROVE");
                    processed++;
                } catch (Exception e) {
                    log.error("Failed to auto-approve trade tx {}: {}", tx.getSeq(), e.getMessage());
                }
            }
        }

        log.info("Trade processing completed. Processed {} trades.", processed);
    }

    @Transactional
    public void processWaivers() {
        log.info("Starting scheduled waiver processing...");

        LocalDateTime cutoffTime = LocalDateTime.now().minusMinutes(30);

        List<RosterTransaction> pendingWaivers = transactionRepository.findByStatusAndType(
                RosterTransaction.TransactionStatus.REQUESTED,
                RosterTransaction.TransactionType.WAIVER);

        int processed = 0;
        java.util.Map<Long, Integer> maxOrdersByGameSeq = new java.util.HashMap<>();
        java.util.Map<Long, java.util.Map<Long, FantasyWaiverOrder>> cachedWaiverOrders = new java.util.HashMap<>();

        for (RosterTransaction tx : pendingWaivers) {
            // Only process if the waiver was requested at least 30 minutes ago
            if (tx.getCreatedAt() != null && tx.getCreatedAt().isBefore(cutoffTime)) {

                List<FantasyWaiverClaim> claims = waiverClaimRepository.findByWaiverSeq(tx.getSeq());

                if (!claims.isEmpty()) {
                    Long gameSeq = tx.getFantasyGameSeq();

                    // Fetch all orders for this gameSeq and cache them
                    java.util.Map<Long, FantasyWaiverOrder> gameOrders = cachedWaiverOrders.computeIfAbsent(gameSeq, key -> {
                        List<FantasyWaiverOrder> orders = waiverOrderRepository.findByGameSeqOrderByOrderNumAsc(key);
                        java.util.Map<Long, FantasyWaiverOrder> map = new java.util.HashMap<>();
                        for (FantasyWaiverOrder order : orders) {
                            map.put(order.getPlayerId(), order);
                        }
                        return map;
                    });

                    // Find the claimer with the lowest orderNum (highest priority)
                    Long bestClaimerId = null;
                    int minOrder = Integer.MAX_VALUE;
                    FantasyWaiverOrder bestClaimerOrder = null;

                    for (FantasyWaiverClaim claim : claims) {
                        FantasyWaiverOrder order = gameOrders.get(claim.getClaimPlayerId());
                        if (order != null) {
                            if (order.getOrderNum() < minOrder) {
                                minOrder = order.getOrderNum();
                                bestClaimerId = claim.getClaimPlayerId();
                                bestClaimerOrder = order;
                            }
                        }
                    }

                    if (bestClaimerId != null) {
                        log.info("Processing waiver tx {} - Claimed by {} with order {}", tx.getSeq(), bestClaimerId, minOrder);

                        try {
                            rosterService.processWaiver(tx.getSeq(), "CLAIM", bestClaimerId);

                            Integer maxOrder = maxOrdersByGameSeq.computeIfAbsent(gameSeq, key -> {
                                Integer dbMax = waiverOrderRepository.findMaxOrderNumByGameSeq(key);
                                return dbMax != null ? dbMax : 0;
                            });

                            int nextOrder = maxOrder + 1;
                            maxOrdersByGameSeq.put(gameSeq, nextOrder); // Update cache for next transactions in same game

                            bestClaimerOrder.setOrderNum(nextOrder);
                            waiverOrderRepository.save(bestClaimerOrder);
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
