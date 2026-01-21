package com.sayai.record.fantasy.controller;

import com.sayai.record.fantasy.dto.FantasyPlayerDto;
import com.sayai.record.fantasy.service.FantasyPlayerService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class FantasyPlayerControllerTest {

    @Mock
    private FantasyPlayerService fantasyPlayerService;

    @InjectMocks
    private FantasyPlayerController fantasyPlayerController;

    @Test
    void updatePlayer_shouldCallService() {
        Long seq = 1L;
        FantasyPlayerDto dto = FantasyPlayerDto.builder().name("Updated Name").build();

        ResponseEntity<String> response = fantasyPlayerController.updatePlayer(seq, dto);

        verify(fantasyPlayerService).updatePlayer(seq, dto);
        assertThat(response.getStatusCodeValue()).isEqualTo(200);
        assertThat(response.getBody()).isEqualTo("Player updated successfully");
    }
}
