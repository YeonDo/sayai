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

        // Fill team with 9 players from "KIA" (4 SP, 4 RP, 1 CL)
        List<FantasyPlayer> currentTeam = new ArrayList<>();
        for(int i=0; i<4; i++) currentTeam.add(createPlayer("KIA", "SP"));
        for(int i=0; i<4; i++) currentTeam.add(createPlayer("KIA", "RP"));
        currentTeam.add(createPlayer("KIA", "CL"));

        // Try to pick 10th player from "KIA"
        FantasyPlayer finalPlayer = createPlayer("KIA", "C");

        assertThatCode(() -> validator.validate(game, finalPlayer, currentTeam, participant))
                .doesNotThrowAnyException();
    }

    @Test
    void validate_shouldFail_whenTeamRestrictionEnabled_andPickMakesItImpossible() {
        FantasyGame game = FantasyGame.builder()
                .useTeamRestriction(true)
                .build();
        FantasyParticipant participant = FantasyParticipant.builder().build();

        // 9 Players from KIA
        List<FantasyPlayer> currentTeam = new ArrayList<>();
        for(int i=0; i<4; i++) currentTeam.add(createPlayer("KIA", "SP"));
        for(int i=0; i<4; i++) currentTeam.add(createPlayer("KIA", "RP"));
        currentTeam.add(createPlayer("KIA", "CL"));

        // Try to pick 10th player from KIA
        // Remaining after this pick: 18 - 10 = 8.
        // Distinct Teams: 1 (KIA). Missing: 9.
        // 8 < 9 -> FAIL
        FantasyPlayer finalPlayer = createPlayer("KIA", "C");

        assertThatThrownBy(() -> validator.validate(game, finalPlayer, currentTeam, participant))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("10개 구단에서 각각 한명씩 뽑아야합니다")
                .hasMessageContaining("빠진 팀 : [")
                .hasMessageContaining("LG")
                .hasMessageContaining("SSG");
    }

    @Test
    void validate_shouldPass_whenTeamRestrictionEnabled_andPickKeepsPossibility() {
        FantasyGame game = FantasyGame.builder()
                .useTeamRestriction(true)
                .build();
        FantasyParticipant participant = FantasyParticipant.builder().build();

        // 9 Players from KIA
        List<FantasyPlayer> currentTeam = new ArrayList<>();
        for(int i=0; i<4; i++) currentTeam.add(createPlayer("KIA", "SP"));
        for(int i=0; i<4; i++) currentTeam.add(createPlayer("KIA", "RP"));
        currentTeam.add(createPlayer("KIA", "CL"));

        // Try to pick 10th player from LG
        // Remaining after this pick: 8.
        // Distinct Teams: 2 (KIA, LG). Missing: 8 (SSG, NC, etc).
        // 8 >= 8 -> PASS
        FantasyPlayer player = createPlayer("LG", "C");

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
