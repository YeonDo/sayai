package com.sayai.record.fantasy.service;

import com.sayai.record.fantasy.entity.FantasyGame;
import com.sayai.record.fantasy.entity.FantasyParticipant;
import com.sayai.record.fantasy.entity.FantasyPlayer;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DraftValidatorTest {

    private final DraftValidator validator = new DraftValidator();

    @Test
    void validate_Rule1_shouldPass_whenSlotsAvailable() {
        FantasyGame game = FantasyGame.builder().ruleType(FantasyGame.RuleType.RULE_1).build();
        FantasyPlayer p1 = FantasyPlayer.builder().position("1B").build();

        // Empty team
        assertThatCode(() -> validator.validate(game, p1, Collections.emptyList(), null))
                .doesNotThrowAnyException();
    }

    @Test
    void validate_Rule1_shouldFail_whenSlotFull() {
        FantasyGame game = FantasyGame.builder().ruleType(FantasyGame.RuleType.RULE_1).build();

        // Team has 1B and DH (hitter) filled
        List<FantasyPlayer> team = new ArrayList<>();
        team.add(FantasyPlayer.builder().position("1B").build()); // Takes 1B
        team.add(FantasyPlayer.builder().position("RF").build()); // Takes RF
        // Fill DH with another hitter
        team.add(FantasyPlayer.builder().position("C").build()); // C
        // ... Assume we fill up everything.
        // Simplest fail: Fill 1B and DH. Then try to add another 1B.

        // Wait, the logic is "Can all players fit?".
        // If I have 1B and DH open.
        // Pick 1: 1B -> Goes to 1B slot. (DH open)
        // Pick 2: 1B -> Goes to DH slot. (Full)
        // Pick 3: 1B -> Fail.

        team.clear();
        team.add(FantasyPlayer.builder().position("1B").build());
        team.add(FantasyPlayer.builder().position("1B").build());

        FantasyPlayer newPlayer = FantasyPlayer.builder().position("1B").build();

        assertThatThrownBy(() -> validator.validate(game, newPlayer, team, null))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Drafting this player violates roster composition rules");
    }

    @Test
    void validate_Rule2_shouldFail_whenFirstPickNotPreferredTeam() {
        FantasyGame game = FantasyGame.builder().ruleType(FantasyGame.RuleType.RULE_2).build();
        FantasyParticipant participant = FantasyParticipant.builder().preferredTeam("TeamA").build();

        FantasyPlayer newPlayer = FantasyPlayer.builder().team("TeamB").build();

        assertThatThrownBy(() -> validator.validate(game, newPlayer, Collections.emptyList(), participant))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("First pick must be from preferred team");
    }

    @Test
    void validate_Rule2_shouldPass_whenFirstPickIsPreferredTeam() {
        FantasyGame game = FantasyGame.builder().ruleType(FantasyGame.RuleType.RULE_2).build();
        FantasyParticipant participant = FantasyParticipant.builder().preferredTeam("TeamA").build();

        FantasyPlayer newPlayer = FantasyPlayer.builder().team("TeamA").position("P").build();

        assertThatCode(() -> validator.validate(game, newPlayer, Collections.emptyList(), participant))
                .doesNotThrowAnyException();
    }
}
