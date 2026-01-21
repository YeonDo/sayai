package com.sayai.record.admin.controller;

import com.sayai.record.fantasy.entity.FantasyGame;
import com.sayai.record.fantasy.repository.FantasyGameRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminControllerTest {

    @Mock
    private FantasyGameRepository fantasyGameRepository;

    @InjectMocks
    private AdminController adminController;

    @Test
    void createGame_shouldSaveGame() {
        AdminController.GameCreateRequest req = new AdminController.GameCreateRequest();
        req.setTitle("New League");
        req.setRuleType(FantasyGame.RuleType.RULE_1);
        req.setScoringType(FantasyGame.ScoringType.POINTS);

        when(fantasyGameRepository.save(any(FantasyGame.class))).thenAnswer(i -> i.getArguments()[0]);

        ResponseEntity<FantasyGame> response = adminController.createGame(req);

        assertThat(response.getBody().getTitle()).isEqualTo("New League");
        assertThat(response.getBody().getScoringType()).isEqualTo(FantasyGame.ScoringType.POINTS);
    }
}
