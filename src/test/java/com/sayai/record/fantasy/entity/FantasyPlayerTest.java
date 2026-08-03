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

    private FantasyPlayer sample() {
        return FantasyPlayer.builder()
                .seq(1L).name("홍길동").position("SS").team("KIA").stats("{}")
                .cost(10).foreignerType(FantasyPlayer.ForeignerType.NONE).isActive(0)
                .build();
    }

    @Test
    void update_shouldApplyOnlyNonNullFields() {
        FantasyPlayer player = sample();
        player.update(null, null, "삼성", null, 25, null, null);

        assertThat(player.getTeam()).isEqualTo("삼성");
        assertThat(player.getCost()).isEqualTo(25);
        assertThat(player.getName()).isEqualTo("홍길동");   // 유지
        assertThat(player.getPosition()).isEqualTo("SS");     // 유지
        assertThat(player.getForeignerType()).isEqualTo(FantasyPlayer.ForeignerType.NONE);
        assertThat(player.getIsActive()).isEqualTo(0);
    }

    @Test
    void update_shouldApplyAllFields() {
        FantasyPlayer player = sample();
        player.update("이순신", "1B", "LG", "{\"hr\":5}", 30, FantasyPlayer.ForeignerType.TYPE_1, 1);

        assertThat(player.getName()).isEqualTo("이순신");
        assertThat(player.getPosition()).isEqualTo("1B");
        assertThat(player.getTeam()).isEqualTo("LG");
        assertThat(player.getStats()).isEqualTo("{\"hr\":5}");
        assertThat(player.getCost()).isEqualTo(30);
        assertThat(player.getForeignerType()).isEqualTo(FantasyPlayer.ForeignerType.TYPE_1);
        assertThat(player.getIsActive()).isEqualTo(1);
    }

    @Test
    void update_shouldThrowException_whenCostIsNegative() {
        FantasyPlayer player = sample();
        assertThatThrownBy(() -> player.update(null, null, null, null, -1, null, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Cost must be non-negative");
        assertThat(player.getCost()).isEqualTo(10); // 예외 시 기존 값 유지
    }
}
