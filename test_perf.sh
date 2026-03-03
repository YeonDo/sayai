cat << 'INNER_EOF' > src/test/java/com/sayai/record/service/CrawlingServiceMockPerfTest.java
package com.sayai.record.service;

import com.sayai.record.model.Player;
import com.sayai.record.repository.PlayerRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

@ExtendWith(MockitoExtension.class)
public class CrawlingServiceMockPerfTest {

    @Mock
    private PlayerRepository playerRepository;

    @InjectMocks
    private PlayerService playerService;

    @Test
    public void testPlayerFetchPerf() {
        // Mock DB calls
        Mockito.when(playerRepository.findPlayerByName(Mockito.anyString()))
               .thenAnswer(invocation -> {
                   // simulate DB latency
                   Thread.sleep(10);
                   return Optional.of(Player.builder().name(invocation.getArgument(0)).build());
               });

        System.out.println("Starting N+1 fetch...");
        long start = System.currentTimeMillis();
        for (int i = 0; i < 50; i++) {
            Optional<Player> p = playerService.getPlayerByName("Player" + i);
        }
        long end = System.currentTimeMillis();
        System.out.println("Time taken with N queries: " + (end - start) + "ms");
    }
}
INNER_EOF
./gradlew test --tests *CrawlingServiceMockPerfTest*
