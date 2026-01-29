package com.sayai.record.security;

import com.sayai.record.fantasy.entity.FantasyPlayer;
import com.sayai.record.fantasy.repository.FantasyPlayerRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
public class SqlInjectionTest {

    @Autowired
    private FantasyPlayerRepository fantasyPlayerRepository;

    @Test
    @DisplayName("Verify SQL Injection Prevention in findPlayers")
    void testSqlInjectionSafety() {
        // Given: A player exists
        FantasyPlayer player = FantasyPlayer.builder()
                .name("SafePlayer")
                .team("KBO")
                .position("P")
                .build();
        fantasyPlayerRepository.save(player);

        // When: Trying to inject SQL via the search parameter
        // If vulnerable, this payload would make the query: ... LIKE '%' OR '1'='1' ...
        // which returns all records.
        // Since it is safe (parameterized), it searches for a name containing "' OR '1'='1".
        String maliciousInput = "' OR '1'='1";
        List<FantasyPlayer> results = fantasyPlayerRepository.findPlayers(null, null, maliciousInput);

        // Then: No results should be found (unless a player literally has that name)
        assertThat(results).isEmpty();

        // Verify positive case to ensure search actually works
        List<FantasyPlayer> validResults = fantasyPlayerRepository.findPlayers(null, null, "Safe");
        assertThat(validResults).hasSize(1);
        assertThat(validResults.get(0).getName()).isEqualTo("SafePlayer");
    }
}
