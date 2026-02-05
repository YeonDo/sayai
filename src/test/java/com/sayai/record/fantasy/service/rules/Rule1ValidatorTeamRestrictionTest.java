package com.sayai.record.fantasy.service.rules;

import com.sayai.record.fantasy.entity.FantasyGame;
import com.sayai.record.fantasy.entity.FantasyParticipant;
import com.sayai.record.fantasy.entity.FantasyPlayer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class Rule1ValidatorTeamRestrictionTest {

    private Rule1Validator validator;

    @BeforeEach
    void setUp() {
        validator = new Rule1Validator();
    }

    @Test
    void validate_shouldPass_whenTeamRestrictionDisabled_andViolatingCondition() {
        FantasyGame game = FantasyGame.builder()
                .useTeamRestriction(false)
                .build();
        FantasyParticipant participant = FantasyParticipant.builder().build();

        // Fill team with 9 players from "TeamA"
        List<FantasyPlayer> currentTeam = new ArrayList<>();
        for (int i = 0; i < 9; i++) {
            currentTeam.add(FantasyPlayer.builder()
                    .seq((long) i)
                    .team("TeamA")
                    .position("P") // Use generic position to pass composition check if possible, or assume mock works
                    .cost(10)
                    .build());
        }

        // Try to pick 10th player from "TeamA"
        // Remaining slots: 18 - 10 = 8.
        // Distinct teams: 1. Missing: 9.
        // 8 < 9. Would fail if enabled.
        FantasyPlayer player = FantasyPlayer.builder()
                .team("TeamA")
                .position("C")
                .cost(10)
                .build();

        // Note: We need to ensure composition check passes.
        // With 9 P and 1 C, it might fail composition if not careful.
        // Rule1Validator checks backtracking.
        // 18 slots total. 4 SP, 4 RP, 1 CL = 9 Pitchers.
        // So 9 "P" might be parsed as SP/RP?
        // Let's rely on the fact that Rule1Validator checks this.
        // We should construct a valid team composition-wise.
        // 4 SP, 4 RP, 1 CL = 9 Players. All from TeamA.
        // 10th Player: C from TeamA.
        // This is valid composition.

        currentTeam.clear();
        for(int i=0; i<4; i++) currentTeam.add(createPlayer("TeamA", "SP"));
        for(int i=0; i<4; i++) currentTeam.add(createPlayer("TeamA", "RP"));
        currentTeam.add(createPlayer("TeamA", "CL"));

        FantasyPlayer finalPlayer = createPlayer("TeamA", "C");

        assertThatCode(() -> validator.validate(game, finalPlayer, currentTeam, participant))
                .doesNotThrowAnyException();
    }

    @Test
    void validate_shouldFail_whenTeamRestrictionEnabled_andPickMakesItImpossible() {
        FantasyGame game = FantasyGame.builder()
                .useTeamRestriction(true)
                .build();
        FantasyParticipant participant = FantasyParticipant.builder().build();

        // 9 Players from TeamA (4 SP, 4 RP, 1 CL)
        List<FantasyPlayer> currentTeam = new ArrayList<>();
        for(int i=0; i<4; i++) currentTeam.add(createPlayer("TeamA", "SP"));
        for(int i=0; i<4; i++) currentTeam.add(createPlayer("TeamA", "RP"));
        currentTeam.add(createPlayer("TeamA", "CL"));

        // Try to pick 10th player from TeamA
        // Remaining after this pick: 18 - 10 = 8.
        // Distinct Teams: 1 (TeamA).
        // Missing Teams: 9.
        // 8 < 9 -> FAIL
        FantasyPlayer player = createPlayer("TeamA", "C");

        assertThatThrownBy(() -> validator.validate(game, player, currentTeam, participant))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Must pick players from at least 10 different teams");
    }

    @Test
    void validate_shouldPass_whenTeamRestrictionEnabled_andPickKeepsPossibility() {
        FantasyGame game = FantasyGame.builder()
                .useTeamRestriction(true)
                .build();
        FantasyParticipant participant = FantasyParticipant.builder().build();

        // 9 Players from TeamA
        List<FantasyPlayer> currentTeam = new ArrayList<>();
        for(int i=0; i<4; i++) currentTeam.add(createPlayer("TeamA", "SP"));
        for(int i=0; i<4; i++) currentTeam.add(createPlayer("TeamA", "RP"));
        currentTeam.add(createPlayer("TeamA", "CL"));

        // Try to pick 10th player from TeamB
        // Remaining after this pick: 8.
        // Distinct Teams: 2 (TeamA, TeamB).
        // Missing Teams: 8.
        // 8 >= 8 -> PASS
        FantasyPlayer player = createPlayer("TeamB", "C");

        assertThatCode(() -> validator.validate(game, player, currentTeam, participant))
                .doesNotThrowAnyException();
    }

    private FantasyPlayer createPlayer(String team, String position) {
        return FantasyPlayer.builder()
                .team(team)
                .position(position)
                .cost(10)
                .build();
    }
}
