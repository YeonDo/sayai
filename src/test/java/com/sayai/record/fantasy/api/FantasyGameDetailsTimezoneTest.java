package com.sayai.record.fantasy.api;

import com.sayai.record.fantasy.entity.FantasyGame;
import com.sayai.record.fantasy.repository.FantasyGameRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.hamcrest.Matchers.matchesPattern;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
public class FantasyGameDetailsTimezoneTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private FantasyGameRepository fantasyGameRepository;

    @Test
    @WithMockUser
    public void getGameDetails_ShouldReturnZonedNextPickDeadline() throws Exception {
        LocalDateTime deadline = LocalDateTime.of(2026, 2, 1, 8, 38, 51);

        FantasyGame game = FantasyGame.builder()
                .title("Timezone Test Game")
                .status(FantasyGame.GameStatus.DRAFTING)
                .ruleType(FantasyGame.RuleType.RULE_1)
                .scoringType(FantasyGame.ScoringType.POINTS)
                .maxParticipants(10)
                .nextPickDeadline(deadline)
                .build();

        game = fantasyGameRepository.save(game);

        mockMvc.perform(get("/apis/v1/fantasy/games/" + game.getSeq() + "/details"))
                .andExpect(status().isOk())
                // Expecting ISO 8601 with Z or +00:00.
                // Since we used ZoneId.of("UTC"), it should likely be "Z" or "+00:00"
                .andExpect(jsonPath("$.nextPickDeadline", matchesPattern(".*(Z|\\+00:00)$")));
    }
}
