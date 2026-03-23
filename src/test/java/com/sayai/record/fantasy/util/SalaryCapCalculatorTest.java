package com.sayai.record.fantasy.util;

import com.sayai.record.fantasy.entity.FantasyGame;
import com.sayai.record.fantasy.entity.FantasyParticipant;
import com.sayai.record.fantasy.entity.FantasyPlayer;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

public class SalaryCapCalculatorTest {

    @Test
    public void testDiscountLogic() {
        FantasyGame game = FantasyGame.builder().useFirstPickRule(true).build();
        FantasyParticipant participant = FantasyParticipant.builder().preferredTeam("KIA").build();

        FantasyPlayer p1 = FantasyPlayer.builder().seq(1L).team("KIA").cost(100).foreignerType(com.sayai.record.fantasy.entity.FantasyPlayer.ForeignerType.NONE).build();
        FantasyPlayer p2 = FantasyPlayer.builder().seq(2L).team("KIA").cost(200).foreignerType(com.sayai.record.fantasy.entity.FantasyPlayer.ForeignerType.NONE).build(); // Highest Domestic
        FantasyPlayer p3 = FantasyPlayer.builder().seq(3L).team("KIA").cost(300).foreignerType(com.sayai.record.fantasy.entity.FantasyPlayer.ForeignerType.TYPE_1).build(); // Higher cost, but Foreigner
        FantasyPlayer p4 = FantasyPlayer.builder().seq(4L).team("LG").cost(250).foreignerType(com.sayai.record.fantasy.entity.FantasyPlayer.ForeignerType.NONE).build(); // Different team

        List<FantasyPlayer> players = Arrays.asList(p1, p2, p3, p4);
        SalaryCapResult result = SalaryCapCalculator.calculateTeamCost(game, participant, players);

        // p1: 100, p2: 100 (discounted), p3: 300, p4: 250 -> Total 750
        assertEquals(750, result.getTotalCost());
        assertEquals(2L, result.getDiscountedPlayerSeq());
        assertEquals(200, result.getOriginalCost());
        assertEquals(100, result.getDiscountedCost());
    }

    @Test
    public void testNoDiscountRule() {
        FantasyGame game = FantasyGame.builder().useFirstPickRule(false).build();
        FantasyParticipant participant = FantasyParticipant.builder().preferredTeam("KIA").build();

        FantasyPlayer p1 = FantasyPlayer.builder().seq(1L).team("KIA").cost(100).foreignerType(com.sayai.record.fantasy.entity.FantasyPlayer.ForeignerType.NONE).build();
        List<FantasyPlayer> players = Arrays.asList(p1);

        SalaryCapResult result = SalaryCapCalculator.calculateTeamCost(game, participant, players);
        assertEquals(100, result.getTotalCost());
        assertNull(result.getDiscountedPlayerSeq());
    }
}
