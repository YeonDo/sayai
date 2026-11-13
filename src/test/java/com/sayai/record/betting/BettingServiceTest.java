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
    void number_specCase() {
        // 명세 6절: 정답 15.2, 풀 100. 점수 8.1 / 7.2 / 0.6125 → 50.9 / 45.2 / 3.8, 내림 후 자투리 2는 1번에
        List<BtLog> logs = List.of(log(1, 1, "15.0", 10), log(2, 2, "17.0", 20), log(3, 3, "13.0", 5),
                log(4, 4, "20.0", 50), log(5, 5, "6.0", 15));
        Map<Long, Long> p = BettingService.computePayouts(BtGame.Type.NUMBER, "15.2", logs);
        assertThat(p).containsEntry(1L, 52L).containsEntry(2L, 45L).containsEntry(3L, 3L)
                .doesNotContainKey(4L).doesNotContainKey(5L);
    }

    @Test
    void number_allSameError_refundsAll() {
        // 14와 16은 정답 15에서 같은 거리 → 한 그룹 → 점수 0 → 전액 환불
        List<BtLog> logs = List.of(log(1, 1, "14.0", 30), log(2, 2, "16.0", 70));
        Map<Long, Long> p = BettingService.computePayouts(BtGame.Type.NUMBER, "15.0", logs);
        assertThat(p).containsEntry(1L, 30L).containsEntry(2L, 70L);
    }

    @Test
    void number_tiesGetSameWeight_regardlessOfOrder_sumEqualsPool() {
        // 부동소수점 함정: 정답 0.3에서 0.1과 0.5는 오차 0.2로 동률이어야 한다. 같은 사람(1)이 여러 번 건 경우 포함
        List<BtLog> logs = List.of(log(1, 1, "0.1", 10), log(2, 1, "0.5", 10), log(3, 2, "0.31", 7),
                log(4, 3, "9.0", 40), log(5, 1, "3.0", 33));
        Map<Long, Long> a = BettingService.computePayouts(BtGame.Type.NUMBER, "0.3", logs);
        Map<Long, Long> b = BettingService.computePayouts(BtGame.Type.NUMBER, "0.3",
                List.of(logs.get(4), logs.get(2), logs.get(1), logs.get(3), logs.get(0)));
        assertThat(a).isEqualTo(b);
        assertThat(a.values().stream().mapToLong(Long::longValue).sum()).isEqualTo(100L);
        // 1·2번은 같은 그룹 → 같은 지급액. 자투리 2는 점수가 가장 큰 3번에 (39 + 2)
        assertThat(a).containsEntry(1L, 28L).containsEntry(2L, 28L).containsEntry(3L, 41L).containsEntry(5L, 3L)
                .doesNotContainKey(4L);
    }

    @Test
    void emptyLogs_nothing() {
        assertThat(BettingService.computePayouts(BtGame.Type.CHOICE, "0", List.of())).isEmpty();
    }
}
