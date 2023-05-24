package com.sayai.record.service;

import com.sayai.record.model.Player;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
@SpringBootTest
class PlayerServiceTest {
    @Autowired
    private PlayerService playerService;
    @Test
    void getPlayer() {
        Optional<Player> player = playerService.getPlayer(24L);
        System.out.println(player.get().getName());
    }


    @Test
    void getAll(){
        List<Player> playerList = playerService.getPlayerList();
    }
}