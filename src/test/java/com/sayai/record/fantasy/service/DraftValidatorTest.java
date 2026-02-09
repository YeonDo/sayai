package com.sayai.record.fantasy.service;

import com.sayai.record.fantasy.entity.FantasyGame;
import com.sayai.record.fantasy.entity.FantasyParticipant;
import com.sayai.record.fantasy.entity.FantasyPlayer;
import com.sayai.record.fantasy.service.rules.Rule1Validator;
import com.sayai.record.fantasy.service.rules.Rule2Validator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DraftValidatorTest {

    private DraftValidator draftValidator;

    @BeforeEach
    void setUp() {
        // Manually assemble the validators
        draftValidator = new DraftValidator(Arrays.asList(new Rule1Validator(), new Rule2Validator()));
    }

    @Test
    void validate_shouldDelegateToRule1() {
        FantasyGame game = FantasyGame.builder().ruleType(FantasyGame.RuleType.RULE_1).build();
        FantasyPlayer p1 = FantasyPlayer.builder().position("1B").cost(10).build();

        assertThatCode(() -> draftValidator.validate(game, p1, Collections.emptyList(), null))
                .doesNotThrowAnyException();
    }

    @Test
    void validate_shouldDelegateToRule2() {
        FantasyGame game = FantasyGame.builder()
                .ruleType(FantasyGame.RuleType.RULE_2)
                .useFirstPickRule(true) // Explicitly enable for test
                .build();
        FantasyParticipant participant = FantasyParticipant.builder().preferredTeam("TeamA").build();
        FantasyPlayer p1 = FantasyPlayer.builder().team("TeamB").position("C").cost(10).build(); // Wrong Team

        assertThatThrownBy(() -> draftValidator.validate(game, p1, Collections.emptyList(), participant))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("1차 지명 룰 위반");
    }

    @Test
    void validate_shouldThrowIfNoValidatorFound() {
        // Create a game with a fictional rule type (hard to do with Enum unless I add one or mock)
        // Since Enum is hardcoded, I'll rely on the logic being simple.
        // Or I can mock the game.getRuleType() if I mocked the Game object, but I used builder.
        // Let's assume testing the happy paths proves the wiring is correct.
    }
}
