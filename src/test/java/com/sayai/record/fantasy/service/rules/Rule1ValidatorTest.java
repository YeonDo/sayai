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

class Rule1ValidatorTest {

    private Rule1Validator validator;

    @BeforeEach
    void setUp() {
        validator = new Rule1Validator();
    }

    @Test
    void validate_shouldPass_whenNullForeignerTypeIsCountedAsNone() {
        FantasyGame game = FantasyGame.builder().build();
        FantasyParticipant participant = FantasyParticipant.builder().build();
        List<FantasyPlayer> currentTeam = new ArrayList<>();

        // Add 3 TYPE_1 foreigners (Max allowed)
        for (int i = 0; i < 3; i++) {
            currentTeam.add(FantasyPlayer.builder().position("1B").foreignerType(FantasyPlayer.ForeignerType.TYPE_1).cost(10).build());
        }

        // Try to add a player with NULL foreigner type -> Should count as NONE and PASS
        // We use a position that fits (e.g. DH if we have slots, but Rule1Validator checks composition too)
        // To simplify composition check failure, we mock valid composition or use simple slots.
        // But since we want to test foreigner limit logic specifically, we can just ensure composition passes or focus on that logic.
        // Actually Rule1Validator.validate calls canFit() first. We need to respect positions.
        // Let's create a minimal team that fits composition but pushes foreigner limit.

        // Actually simpler: Testing validateForeignerLimits is private, so we test via validate.
        // We need to construct a team that satisfies `canFit` to reach `validateForeignerLimits`.
        // This might be complex due to backtracking.

        // Alternative: Use reflection to test private method or trust that if I provide valid positions it works.
        // Let's try to provide valid positions.
        // 3 TYPE_1: 1B, 2B, 3B
        // New Player: Null Type (NONE), Position SS.
        // Total 4 players. All fit into C, 1B, 2B, SS, 3B etc.

        List<FantasyPlayer> team = new ArrayList<>();
        team.add(FantasyPlayer.builder().position("1B").foreignerType(FantasyPlayer.ForeignerType.TYPE_1).cost(10).build());
        team.add(FantasyPlayer.builder().position("2B").foreignerType(FantasyPlayer.ForeignerType.TYPE_1).cost(10).build());
        team.add(FantasyPlayer.builder().position("3B").foreignerType(FantasyPlayer.ForeignerType.TYPE_1).cost(10).build());

        FantasyPlayer newPlayer = FantasyPlayer.builder().position("SS").foreignerType(null).cost(10).build();

        assertThatCode(() -> validator.validate(game, newPlayer, team, participant))
                .doesNotThrowAnyException();
    }
}
