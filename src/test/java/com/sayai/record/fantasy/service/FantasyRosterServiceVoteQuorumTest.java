package com.sayai.record.fantasy.service;

import com.sayai.record.fantasy.entity.FantasyParticipant;
import com.sayai.record.fantasy.entity.RosterTransaction;
import com.sayai.record.fantasy.entity.TradeBoard;
import com.sayai.record.fantasy.repository.FantasyParticipantRepository;
import com.sayai.record.fantasy.repository.RosterTransactionRepository;
import com.sayai.record.fantasy.repository.TradeBoardRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * 봇이 포함된 대회에서 트레이드 투표 정족수가 봇을 제외하고 계산되는지 검증.
 * 봇을 정족수(n)에 포함하던 기존 버그였다면 approve/reject 임계값이 커져
 * 아래 테스트의 processTrade 호출 검증이 실패한다 (regression guard).
 */
@ExtendWith(MockitoExtension.class)
public class FantasyRosterServiceVoteQuorumTest {

    @Mock private RosterTransactionRepository rosterTransactionRepository;
    @Mock private FantasyParticipantRepository fantasyParticipantRepository;
    @Mock private TradeBoardRepository tradeBoardRepository;

    @Spy
    @InjectMocks
    private FantasyRosterService service;

    private static final Long GAME = 1L;
    private static final Long TX = 100L;
    private static final Long REQUESTER = 1L;
    private static final Long TARGET = 2L;

    private RosterTransaction newTransaction() {
        try {
            var c = RosterTransaction.class.getDeclaredConstructor();
            c.setAccessible(true);
            return c.newInstance();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /** 참가자 6명 = 봇 2명 + 실유저 4명(당사자 2 + 투표자 2). 봇 제외 시 n=4. */
    private void stubGameWithTwoBots(Long voterId) {
        RosterTransaction tx = newTransaction();
        ReflectionTestUtils.setField(tx, "seq", TX);
        ReflectionTestUtils.setField(tx, "fantasyGameSeq", GAME);
        ReflectionTestUtils.setField(tx, "type", RosterTransaction.TransactionType.TRADE);
        ReflectionTestUtils.setField(tx, "status", RosterTransaction.TransactionStatus.REQUESTED);
        ReflectionTestUtils.setField(tx, "requesterId", REQUESTER);
        ReflectionTestUtils.setField(tx, "targetId", TARGET);
        when(rosterTransactionRepository.findById(TX)).thenReturn(Optional.of(tx));

        when(fantasyParticipantRepository.findByFantasyGameSeqAndMemberId(GAME, voterId))
                .thenReturn(Optional.of(participant(voterId, false)));
        when(tradeBoardRepository.findByTradeSeqAndMemberId(TX, voterId)).thenReturn(Optional.empty());

        List<FantasyParticipant> all = new ArrayList<>(List.of(
                participant(REQUESTER, false), participant(TARGET, false),
                participant(3L, false), participant(4L, false),
                participant(90L, true), participant(91L, true)));
        when(fantasyParticipantRepository.findByFantasyGameSeq(GAME)).thenReturn(all);

        // processTrade는 정족수 도달 여부만 확인하면 되므로 본문 실행을 막는다
        // (미결 케이스에선 호출되지 않으므로 lenient)
        lenient().doNothing().when(service).processTrade(anyLong(), anyString());
    }

    private FantasyParticipant participant(Long memberId, boolean bot) {
        return FantasyParticipant.builder()
                .fantasyGameSeq(GAME).memberId(memberId).isBot(bot).build();
    }

    private TradeBoard vote(Long memberId, boolean agree) {
        return TradeBoard.builder().tradeSeq(TX).memberId(memberId).voteAgree(agree).build();
    }

    @Test
    void 봇제외_실유저_과반_찬성이면_트레이드_승인() {
        // n(실유저)=4, approveThreshold=(4-2)/2+1=2. 투표자 2명 모두 찬성 → 승인.
        stubGameWithTwoBots(4L);
        when(tradeBoardRepository.findByTradeSeq(TX))
                .thenReturn(List.of(vote(3L, true), vote(4L, true)));

        service.voteOnTrade(GAME, TX, 4L, true);

        verify(service).processTrade(TX, "APPROVE");
    }

    @Test
    void 봇제외_실유저_과반_반대면_트레이드_거절() {
        // n(실유저)=4, rejectThreshold=ceil((4-2)/2)=1. 반대 1표 → 거절.
        stubGameWithTwoBots(3L);
        when(tradeBoardRepository.findByTradeSeq(TX))
                .thenReturn(List.of(vote(3L, false)));

        service.voteOnTrade(GAME, TX, 3L, false);

        verify(service).processTrade(TX, "REJECT");
    }

    @Test
    void 투표자_1명만_찬성이면_승인_안됨() {
        // n(실유저)=4, approveThreshold=2. 찬성 1표뿐 → 아직 미결.
        stubGameWithTwoBots(3L);
        when(tradeBoardRepository.findByTradeSeq(TX))
                .thenReturn(List.of(vote(3L, true)));

        service.voteOnTrade(GAME, TX, 3L, true);

        verify(service, never()).processTrade(eq(TX), anyString());
    }
}
