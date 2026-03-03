package com.sayai.record.service;

import com.sayai.record.model.Player;
import com.sayai.record.repository.PlayerRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@ExtendWith(MockitoExtension.class)
public class CrawlingServiceMockPerfTest {

    @Mock
    private PlayerRepository playerRepository;

    @InjectMocks
    private PlayerService playerService;

    @Test
    public void testPlayerFetchPerf() throws Exception {
        // N+1 style
        Mockito.when(playerRepository.findPlayerByName(Mockito.anyString()))
               .thenAnswer(invocation -> {
                   Thread.sleep(10);
                   return Optional.of(Player.builder().name(invocation.getArgument(0)).build());
               });

        System.out.println("Starting N+1 fetch...");
        long start1 = System.currentTimeMillis();
        for (int i = 0; i < 50; i++) {
            Optional<Player> p = playerService.getPlayerByName("Player" + i);
        }
        long end1 = System.currentTimeMillis();
        System.out.println("Time taken with N queries: " + (end1 - start1) + "ms");

        // Batch style
        Mockito.when(playerRepository.findAll())
               .thenAnswer(invocation -> {
                   Thread.sleep(10); // single query latency
                   return IntStream.range(0, 50)
                           .mapToObj(i -> Player.builder().name("Player" + i).sleepYn("N").build())
                           .collect(Collectors.toList());
               });

        System.out.println("Starting batched fetch...");
        long start2 = System.currentTimeMillis();

        List<Player> allPlayers = playerRepository.findAll();
        Map<String, Player> playerCache = allPlayers.stream()
            .collect(Collectors.toMap(
                p -> "임환용".equals(p.getName()) ? "임강록" : p.getName(),
                Function.identity(),
                (p1, p2) -> p1
            ));

        for (int i = 0; i < 50; i++) {
            String name = "Player" + i;
            if ("임환용".equals(name)) name = "임강록";
            Player p = playerCache.get(name);
        }
        long end2 = System.currentTimeMillis();
        System.out.println("Time taken with batch query: " + (end2 - start2) + "ms");
    }
}
