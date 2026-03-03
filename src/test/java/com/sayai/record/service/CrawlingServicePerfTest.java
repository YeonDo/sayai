package com.sayai.record.service;

import com.sayai.record.model.Player;
import com.sayai.record.repository.PlayerRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@SpringBootTest
@ActiveProfiles("local")
public class CrawlingServicePerfTest {

    @Autowired
    private PlayerService playerService;

    @Autowired
    private PlayerRepository playerRepository;

    @Test
    @Transactional
    public void testPlayerFetchPerf() {
        // Setup mock players
        for (int i = 0; i < 50; i++) {
            playerRepository.save(Player.builder().name("Player" + i).build());
        }

        long start = System.currentTimeMillis();
        for (int i = 0; i < 50; i++) {
            Optional<Player> p = playerService.getPlayerByName("Player" + i);
        }
        long end = System.currentTimeMillis();
        System.out.println("Time taken: " + (end - start) + "ms");
    }
}
