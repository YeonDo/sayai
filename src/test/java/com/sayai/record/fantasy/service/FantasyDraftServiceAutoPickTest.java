package com.sayai.record.fantasy.service;

import com.sayai.kbo.repository.KboHitterStatsRepository;
import com.sayai.kbo.repository.KboPitcherStatsRepository;
import com.sayai.record.fantasy.entity.*;
import com.sayai.record.fantasy.repository.DraftPickRepository;
import com.sayai.record.fantasy.repository.FantasyGameRepository;
import com.sayai.record.fantasy.repository.FantasyParticipantRepository;
import com.sayai.record.fantasy.repository.FantasyPlayerRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * 오토픽 로직은 FantasyDraftService → DraftAutoPickService 로 이동했고,
 * 실제 픽 커밋은 DraftPickExecutor.commitPick 에 위임된다.
 * 이 테스트는 룰이 비활성일 때 후보가 팀으로 필터링되지 않고 픽이 커밋되는지 검증한다.
 */
@ExtendWith(MockitoExtension.class)
class FantasyDraftServiceAutoPickTest {

    @Mock private FantasyPlayerRepository fantasyPlayerRepository;
    @Mock private DraftPickRepository draftPickRepository;
    @Mock private FantasyGameRepository fantasyGameRepository;
    @Mock private FantasyParticipantRepository fantasyParticipantRepository;
    @Mock private DraftPickExecutor draftPickExecutor;
    @Mock private DraftValidator draftValidator;
    @Mock private KboHitterStatsRepository kboHitterStatsRepository;
    @Mock private KboPitcherStatsRepository kboPitcherStatsRepository;

    @InjectMocks
    private DraftAutoPickService draftAutoPickService;

    @Test
    void autoPick_FirstPickRule_Disabled_커밋됨() {
        Long gameSeq = 1L;
        Long playerId = 100L;

        FantasyGame game = FantasyGame.builder()
                .seq(gameSeq)
                .status(FantasyGame.GameStatus.DRAFTING)
                .ruleType(FantasyGame.RuleType.RULE_2)
                .useFirstPickRule(false) // 비활성 → 선호팀 필터 없음
                .build();
        when(fantasyGameRepository.findById(gameSeq)).thenReturn(Optional.of(game));

        // 유저 차례(봇 아님), 1라운드
        DraftPickExecutor.NextPickInfo info = new DraftPickExecutor.NextPickInfo();
        info.pickerId = playerId;
        info.round = 1;
        info.pickInRound = 1;
        info.isBot = false;
        when(draftPickExecutor.getNextPickInfo(game)).thenReturn(info);

        // 아직 픽 없음 → 전체 활성 선수 대상
        when(draftPickRepository.findByFantasyGameSeq(gameSeq)).thenReturn(Collections.emptyList());
        FantasyPlayer p1 = FantasyPlayer.builder().seq(1L).name("P1").team("Samsung").cost(10).position("C").build();
        when(fantasyPlayerRepository.findAllActivePlayers()).thenReturn(List.of(p1));

        FantasyParticipant participant = FantasyParticipant.builder()
                .memberId(playerId).draftOrder(1).preferredTeam("KIA").build();
        when(fantasyParticipantRepository.findByFantasyGameSeqAndMemberId(gameSeq, playerId))
                .thenReturn(Optional.of(participant));

        // 포지션 슬롯 여유 있음 → 후보에 포함
        when(draftPickExecutor.hasOpenSlot(anyList(), any())).thenReturn(true);
        // 샐캡 null → 검증만 통과시키면 선택됨
        doNothing().when(draftValidator).validate(any(), any(), any(), any());

        draftAutoPickService.autoPick(gameSeq);

        // 룰 비활성이므로 Samsung 소속 P1이 팀 필터에 걸러지지 않고 커밋되어야 함
        verify(draftPickExecutor, times(1))
                .commitPick(eq(game), any(), eq(p1), eq(true), eq(info));
    }
}
