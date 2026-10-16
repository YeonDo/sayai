package com.sayai.record.betting;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class BettingServiceTest {

    private static BtLog log(long id, long member, String answer, long amount) {
        return BtLog.builder().id(id).btGameId(1L).memberId(member).answer(answer).amount(amount).build();
    }

    @Test
    void choice_payoutIsProportionalToTotalPool() {
        // 풀 300, 정답 "0"에 100(두 명 50씩) → 배당 3배
        List<BtLog> logs = List.of(log(1, 1, "0", 50), log(2, 2, "0", 50), log(3, 3, "1", 200));
        Map<Long, Long> p = BettingService.computePayouts(BtGame.Type.CHOICE, "0", logs);
        assertThat(p).containsEntry(1L, 150L).containsEntry(2L, 150L).doesNotContainKey(3L);
    }

    @Test
    void choice_noWinner_refundsAll() {
        List<BtLog> logs = List.of(log(1, 1, "0", 50), log(2, 2, "1", 70));
        Map<Long, Long> p = BettingService.computePayouts(BtGame.Type.CHOICE, "2", logs);
        assertThat(p).containsEntry(1L, 50L).containsEntry(2L, 70L);
    }

    @Test
    void number_nearestWins_tiesShare() {
        // 정답 10: 9와 11이 동률 적중, 20은 낙첨. 풀 100, 적중풀 50 → 2배
        List<BtLog> logs = List.of(log(1, 1, "9.0", 20), log(2, 2, "11.0", 30), log(3, 3, "20.0", 50));
        Map<Long, Long> p = BettingService.computePayouts(BtGame.Type.NUMBER, "10.0", logs);
        assertThat(p).containsEntry(1L, 40L).containsEntry(2L, 60L).doesNotContainKey(3L);
    }

    @Test
    void emptyLogs_nothing() {
        assertThat(BettingService.computePayouts(BtGame.Type.CHOICE, "0", List.of())).isEmpty();
    }
}
