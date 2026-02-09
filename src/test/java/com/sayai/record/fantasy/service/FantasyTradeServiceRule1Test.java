package com.sayai.record.fantasy.service;

import com.sayai.record.fantasy.dto.TradeProposalDto;
import com.sayai.record.fantasy.entity.FantasyGame;
import com.sayai.record.fantasy.repository.FantasyGameRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

class FantasyTradeServiceRule1Test {

    @Mock private FantasyGameRepository fantasyGameRepository;
    // Other mocks not needed for this specific check as it fails early

    @InjectMocks
    private FantasyTradeService fantasyTradeService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void testDropPlayer_Rule1_ThrowsException() {
        Long gameSeq = 1L;
        FantasyGame game = FantasyGame.builder().status(FantasyGame.GameStatus.ONGOING).ruleType(FantasyGame.RuleType.RULE_1).build();
        when(fantasyGameRepository.findById(gameSeq)).thenReturn(Optional.of(game));

        assertThrows(IllegalStateException.class, () -> fantasyTradeService.dropPlayer(gameSeq, 100L, 500L));
    }

    @Test
    void testClaimPlayer_Rule1_ThrowsException() {
        Long gameSeq = 1L;
        FantasyGame game = FantasyGame.builder().status(FantasyGame.GameStatus.ONGOING).ruleType(FantasyGame.RuleType.RULE_1).build();
        when(fantasyGameRepository.findById(gameSeq)).thenReturn(Optional.of(game));

        assertThrows(IllegalStateException.class, () -> fantasyTradeService.claimPlayer(gameSeq, 100L, 500L));
    }

    @Test
    void testAssignPlayerByAdmin_Rule1_ThrowsException() {
        Long gameSeq = 1L;
        FantasyGame game = FantasyGame.builder().status(FantasyGame.GameStatus.ONGOING).ruleType(FantasyGame.RuleType.RULE_1).build();
        when(fantasyGameRepository.findById(gameSeq)).thenReturn(Optional.of(game));

        assertThrows(IllegalStateException.class, () -> fantasyTradeService.assignPlayerByAdmin(gameSeq, 100L, 500L));
    }

    @Test
    void testProposeTrade_Rule1_ThrowsException() {
        Long gameSeq = 1L;
        FantasyGame game = FantasyGame.builder().status(FantasyGame.GameStatus.ONGOING).ruleType(FantasyGame.RuleType.RULE_1).build();
        when(fantasyGameRepository.findById(gameSeq)).thenReturn(Optional.of(game));

        TradeProposalDto dto = new TradeProposalDto();
        dto.setGameSeq(gameSeq);

        assertThrows(IllegalStateException.class, () -> fantasyTradeService.proposeTrade(100L, dto));
    }
}
