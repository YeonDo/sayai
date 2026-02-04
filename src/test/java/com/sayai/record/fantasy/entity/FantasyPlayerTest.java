package com.sayai.record.fantasy.entity;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.assertThat;

class FantasyPlayerTest {

    @Test
    void builder_shouldThrowException_whenCostIsNegative() {
        assertThatThrownBy(() -> FantasyPlayer.builder()
                .cost(-100)
                .build())
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Cost must be non-negative");
    }

    @Test
    void setCost_shouldThrowException_whenCostIsNegative() {
        FantasyPlayer player = new FantasyPlayer();
        assertThatThrownBy(() -> player.setCost(-50))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Cost must be non-negative");
    }

    @Test
    void setCost_shouldThrowException_whenCostIsNull() {
        FantasyPlayer player = new FantasyPlayer();
        assertThatThrownBy(() -> player.setCost(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Cost must be non-negative");
    }

    @Test
    void setCost_shouldSucceed_whenCostIsZeroOrPositive() {
        FantasyPlayer player = new FantasyPlayer();
        player.setCost(0);
        assertThat(player.getCost()).isEqualTo(0);

        player.setCost(100);
        assertThat(player.getCost()).isEqualTo(100);
    }
}
