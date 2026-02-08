package com.sayai.record.fantasy.service.rules;

import com.sayai.record.fantasy.entity.FantasyGame;
import com.sayai.record.fantasy.entity.FantasyParticipant;
import com.sayai.record.fantasy.entity.FantasyPlayer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class Rule2ValidatorTest {

    private Rule2Validator validator;
    private FantasyGame game;
    private FantasyParticipant participant;

    @BeforeEach
    void setUp() {
        validator = new Rule2Validator();
        game = FantasyGame.builder()
                .ruleType(FantasyGame.RuleType.RULE_2)
                .useFirstPickRule(false) // Default disabled
                .build();
        participant = FantasyParticipant.builder()
                .preferredTeam("KIA")
                .build();
    }

    @Test
    void testTotalPlayerCount() {
        assertEquals(20, validator.getTotalPlayerCount());
    }

    @Test
    void testFirstPickRule_Success() {
        // Enable Rule
        game.setUseFirstPickRule(true);
        FantasyPlayer p = FantasyPlayer.builder().team("KIA Tigers").position("C").cost(10).build();
        assertDoesNotThrow(() -> validator.validate(game, p, Collections.emptyList(), participant));
    }

    @Test
    void testFirstPickRule_Fail() {
        // Enable Rule
        game.setUseFirstPickRule(true);
        FantasyPlayer p = FantasyPlayer.builder().team("Samsung Lions").position("C").cost(10).build();
        assertThrows(IllegalStateException.class, () -> validator.validate(game, p, Collections.emptyList(), participant));
    }

    @Test
    void testFirstPickRule_Disabled() {
        // Disable Rule (default)
        game.setUseFirstPickRule(false);
        FantasyPlayer p = FantasyPlayer.builder().team("Samsung Lions").position("C").cost(10).build();
        assertDoesNotThrow(() -> validator.validate(game, p, Collections.emptyList(), participant));
    }

    @Test
    void testValidComposition() {
        // Create 18 mandatory players
        List<FantasyPlayer> team = new ArrayList<>();
        team.add(createPlayer("C", "C"));
        team.add(createPlayer("1B", "1B"));
        team.add(createPlayer("2B", "2B"));
        team.add(createPlayer("3B", "3B"));
        team.add(createPlayer("SS", "SS"));
        team.add(createPlayer("LF", "LF"));
        team.add(createPlayer("CF", "CF"));
        team.add(createPlayer("RF", "RF"));
        team.add(createPlayer("DH", "DH"));

        // 4 SP, 4 RP, 1 CL
        for(int i=0; i<4; i++) team.add(createPlayer("SP", "SP"));
        for(int i=0; i<4; i++) team.add(createPlayer("RP", "RP"));
        team.add(createPlayer("CL", "CL"));

        // 18 players so far.
        // Add 1 extra player (Bench)
        team.add(createPlayer("ExtraC", "C"));

        // Add last player (Bench)
        FantasyPlayer last = createPlayer("ExtraSP", "SP");

        assertDoesNotThrow(() -> validator.validate(game, last, team, participant));
    }

    @Test
    void testInvalidComposition_MissingMandatory() {
        // 19 players, but missing 1B (replaced by something else)
        List<FantasyPlayer> team = new ArrayList<>();
        team.add(createPlayer("C", "C"));
        // No 1B
        team.add(createPlayer("2B", "2B")); // Extra 2B
        team.add(createPlayer("2B", "2B"));
        team.add(createPlayer("3B", "3B"));
        team.add(createPlayer("SS", "SS"));
        team.add(createPlayer("LF", "LF"));
        team.add(createPlayer("CF", "CF"));
        team.add(createPlayer("RF", "RF"));
        team.add(createPlayer("DH", "DH"));

        for(int i=0; i<4; i++) team.add(createPlayer("SP", "SP"));
        for(int i=0; i<4; i++) team.add(createPlayer("RP", "RP"));
        team.add(createPlayer("CL", "CL"));

        // Add bench to reach 19
        team.add(createPlayer("Bench1", "C"));
        team.add(createPlayer("Bench2", "C"));

        // Try adding a non-1B player as 20th.
        FantasyPlayer p = createPlayer("Not1B", "C");

        // Should fail because even with 20 players, we haven't filled 1B slot.
        assertThrows(IllegalStateException.class, () -> validator.validate(game, p, team, participant));
    }

    private FantasyPlayer createPlayer(String name, String pos) {
        return FantasyPlayer.builder()
                .name(name)
                .position(pos)
                .team("KIA") // Match preferred
                .cost(10)
                .build();
    }
}
