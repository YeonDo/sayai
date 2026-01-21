package com.sayai.record.fantasy.entity;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class DraftPickTest {

    @Test
    void builder_shouldSetPickNumber() {
        DraftPick pick = DraftPick.builder()
                .fantasyGameSeq(1L)
                .pickNumber(5)
                .build();

        assertThat(pick.getPickNumber()).isEqualTo(5);
    }
}
