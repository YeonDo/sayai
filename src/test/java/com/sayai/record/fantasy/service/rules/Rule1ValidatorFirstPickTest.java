package com.sayai.record.fantasy.service.rules;

import com.sayai.record.fantasy.entity.FantasyGame;
import com.sayai.record.fantasy.entity.FantasyParticipant;
import com.sayai.record.fantasy.entity.FantasyPlayer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class Rule1ValidatorFirstPickTest {

    private Rule1Validator validator;

    @BeforeEach
    void setUp() {
        validator = new Rule1Validator();
    }

    @Test
    void validate_shouldFail_whenFirstPickRuleEnabled_andPlayerNotFromPreferredTeam() {
        FantasyGame game = FantasyGame.builder()
                .useFirstPickRule(true)
                .build();
        FantasyParticipant participant = FantasyParticipant.builder()
                .preferredTeam("Doosan")
                .build();
        FantasyPlayer player = FantasyPlayer.builder()
                .team("Samsung")
                .position("1B")
                .cost(10)
                .build();
        List<FantasyPlayer> currentTeam = Collections.emptyList();

        assertThatThrownBy(() -> validator.validate(game, player, currentTeam, participant))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("First pick must be from preferred team");
    }

    @Test
    void validate_shouldPass_whenFirstPickRuleEnabled_andPlayerFromPreferredTeam() {
        FantasyGame game = FantasyGame.builder()
                .useFirstPickRule(true)
                .build();
        FantasyParticipant participant = FantasyParticipant.builder()
                .preferredTeam("Doosan")
                .build();
        FantasyPlayer player = FantasyPlayer.builder()
                .team("Doosan")
                .position("1B")
                .cost(10)
                .build();
        List<FantasyPlayer> currentTeam = Collections.emptyList();

        assertThatCode(() -> validator.validate(game, player, currentTeam, participant))
                .doesNotThrowAnyException();
    }

    @Test
    void validate_shouldPass_whenFirstPickRuleDisabled_andPlayerNotFromPreferredTeam() {
        FantasyGame game = FantasyGame.builder()
                .useFirstPickRule(false)
                .build();
        FantasyParticipant participant = FantasyParticipant.builder()
                .preferredTeam("Doosan")
                .build();
        FantasyPlayer player = FantasyPlayer.builder()
                .team("Samsung")
                .position("1B")
                .cost(10)
                .build();
        List<FantasyPlayer> currentTeam = Collections.emptyList();

        assertThatCode(() -> validator.validate(game, player, currentTeam, participant))
                .doesNotThrowAnyException();
    }

    @Test
    void validate_shouldPass_whenNotFirstPick() {
        FantasyGame game = FantasyGame.builder()
                .useFirstPickRule(true)
                .build();
        FantasyParticipant participant = FantasyParticipant.builder()
                .preferredTeam("Doosan")
                .build();

        // Existing team with 1 player
        List<FantasyPlayer> currentTeam = new ArrayList<>();
        currentTeam.add(FantasyPlayer.builder().position("1B").team("Doosan").cost(10).build());

        FantasyPlayer player = FantasyPlayer.builder()
                .team("Samsung")
                .position("2B")
                .cost(10)
                .build();

        assertThatCode(() -> validator.validate(game, player, currentTeam, participant))
                .doesNotThrowAnyException();
    }
}
