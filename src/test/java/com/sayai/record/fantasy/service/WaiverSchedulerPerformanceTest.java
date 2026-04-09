package com.sayai.record.fantasy.service;

import com.sayai.record.fantasy.entity.FantasyWaiverClaim;
import com.sayai.record.fantasy.entity.FantasyWaiverOrder;
import com.sayai.record.fantasy.entity.RosterTransaction;
import com.sayai.record.fantasy.repository.FantasyWaiverClaimRepository;
import com.sayai.record.fantasy.repository.FantasyWaiverOrderRepository;
import com.sayai.record.fantasy.repository.RosterTransactionRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class WaiverSchedulerPerformanceTest {

    @Mock
    private RosterTransactionRepository transactionRepository;
    @Mock
    private FantasyWaiverClaimRepository waiverClaimRepository;
    @Mock
    private FantasyWaiverOrderRepository waiverOrderRepository;
    @Mock
    private FantasyRosterService rosterService;

    @InjectMocks
    private WaiverScheduler waiverScheduler;

    private <T> T instantiate(Class<T> clazz) {
        try {
            java.lang.reflect.Constructor<T> constructor = clazz.getDeclaredConstructor();
            constructor.setAccessible(true);
            return constructor.newInstance();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    public void benchmarkProcessWaivers() throws Exception {
        int numTransactions = 1000;
        List<RosterTransaction> pendingWaivers = new ArrayList<>();

        for (long i = 1; i <= numTransactions; i++) {
            RosterTransaction tx = instantiate(RosterTransaction.class);
            ReflectionTestUtils.setField(tx, "seq", i);
            ReflectionTestUtils.setField(tx, "fantasyGameSeq", 1L);
            ReflectionTestUtils.setField(tx, "createdAt", LocalDateTime.now().minusHours(1));
            pendingWaivers.add(tx);

            FantasyWaiverClaim claim = instantiate(FantasyWaiverClaim.class);
            ReflectionTestUtils.setField(claim, "claimPlayerId", i);
            when(waiverClaimRepository.findById(i)).thenReturn(Optional.of(claim));

            FantasyWaiverOrder order = instantiate(FantasyWaiverOrder.class);
            when(waiverOrderRepository.findByGameSeqAndPlayerId(1L, i)).thenReturn(Optional.of(order));
        }

        when(transactionRepository.findByStatusAndType(
                RosterTransaction.TransactionStatus.REQUESTED,
                RosterTransaction.TransactionType.WAIVER)).thenReturn(pendingWaivers);

        when(waiverOrderRepository.findMaxOrderNumByGameSeq(1L)).thenReturn(10);

        long startTime = System.currentTimeMillis();
        waiverScheduler.processWaivers();
        long endTime = System.currentTimeMillis();

        System.out.println("Processing " + numTransactions + " waivers took: " + (endTime - startTime) + " ms");

        verify(waiverOrderRepository, atLeast(1)).findMaxOrderNumByGameSeq(1L);
    }
}
