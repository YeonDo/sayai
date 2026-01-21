package com.sayai.record.fantasy.entity;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class FantasyEntityTest {

    @Test
    void testFantasyGameBuilder() {
        FantasyGame game = FantasyGame.builder()
                .title("League 1")
                .status(FantasyGame.GameStatus.WAITING)
                .build();

        assertThat(game.getTitle()).isEqualTo("League 1");
        assertThat(game.getStatus()).isEqualTo(FantasyGame.GameStatus.WAITING);
    }

    @Test
    void testDraftPickBuilder() {
        DraftPick pick = DraftPick.builder()
                .playerId(100L)
                .fantasyPlayerSeq(1L)
                .fantasyGameSeq(5L)
                .build();

        assertThat(pick.getPlayerId()).isEqualTo(100L);
        assertThat(pick.getFantasyPlayerSeq()).isEqualTo(1L);
        assertThat(pick.getFantasyGameSeq()).isEqualTo(5L);
    }
}
