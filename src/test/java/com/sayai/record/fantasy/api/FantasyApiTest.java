package com.sayai.record.fantasy.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sayai.record.admin.controller.AdminController;
import com.sayai.record.fantasy.controller.FantasyDraftController;
import com.sayai.record.fantasy.entity.FantasyGame;
import com.sayai.record.fantasy.repository.FantasyGameRepository;
import com.sayai.record.fantasy.repository.FantasyParticipantRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
public class FantasyApiTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private FantasyGameRepository fantasyGameRepository;

    @Test
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    public void testCreateGame_Admin() throws Exception {
        AdminController.GameCreateRequest request = new AdminController.GameCreateRequest();
        request.setTitle("Test League");
        request.setRuleType(FantasyGame.RuleType.RULE_1);
        request.setScoringType(FantasyGame.ScoringType.POINTS);
        request.setScoringSettings("{}");
        request.setMaxParticipants(10);
        request.setDraftDate(LocalDateTime.now().plusDays(1));

        mockMvc.perform(post("/apis/v1/admin/games")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Test League"))
                .andExpect(jsonPath("$.status").value("WAITING"));
    }

    @Test
    @WithMockUser(username = "user")
    public void testGetGames() throws Exception {
        FantasyGame game = FantasyGame.builder()
                .title("Dashboard Game")
                .status(FantasyGame.GameStatus.WAITING)
                .ruleType(FantasyGame.RuleType.RULE_1)
                .scoringType(FantasyGame.ScoringType.POINTS)
                .maxParticipants(10)
                .draftDate(LocalDateTime.now().plusDays(2))
                .build();
        fantasyGameRepository.save(game);

        mockMvc.perform(get("/apis/v1/fantasy/games")
                        .param("userId", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].title").value("Dashboard Game"));
    }

    @Test
    @WithMockUser(username = "user")
    public void testJoinGame() throws Exception {
        FantasyGame game = FantasyGame.builder()
                .title("Joinable Game")
                .status(FantasyGame.GameStatus.WAITING)
                .ruleType(FantasyGame.RuleType.RULE_1)
                .scoringType(FantasyGame.ScoringType.POINTS)
                .maxParticipants(10)
                .draftDate(LocalDateTime.now().plusDays(2))
                .build();
        game = fantasyGameRepository.save(game);

        // Updated constructor to match new signature if needed, or use setters
        FantasyDraftController.JoinRequest request = new FantasyDraftController.JoinRequest(1L, "Doosan", "My Team");

        mockMvc.perform(post("/apis/v1/fantasy/games/" + game.getSeq() + "/join")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(content().string("Joined successfully"));
    }
}
