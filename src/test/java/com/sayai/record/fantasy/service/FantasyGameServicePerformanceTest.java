package com.sayai.record.fantasy.service;

import com.sayai.record.fantasy.entity.FantasyGame;
import com.sayai.record.fantasy.entity.FantasyParticipant;
import com.sayai.record.fantasy.repository.DraftPickRepository;
import com.sayai.record.fantasy.repository.FantasyGameRepository;
import com.sayai.record.fantasy.repository.FantasyParticipantRepository;
import com.sayai.record.fantasy.repository.FantasyPlayerRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.util.ArrayList;
import java.util.List;

@DataJpaTest
class FantasyGameServicePerformanceTest {

    @Autowired
    private FantasyGameRepository fantasyGameRepository;

    @Autowired
    private FantasyParticipantRepository fantasyParticipantRepository;

    @Mock
    private DraftPickRepository draftPickRepository;

    @Mock
    private com.sayai.record.fantasy.repository.DraftPickSnapshotRepository draftPickSnapshotRepository;

    @Mock
    private FantasyPlayerRepository fantasyPlayerRepository;

    @Mock
    private SimpMessagingTemplate messagingTemplate;

    @Mock
    private FantasyDraftService fantasyDraftService;

    @Mock
    private DraftScheduler draftScheduler;

    private FantasyGameService fantasyGameService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        fantasyGameService = new FantasyGameService(
                fantasyGameRepository,
                fantasyParticipantRepository,
                draftPickRepository,
                draftPickSnapshotRepository,
                fantasyPlayerRepository,
                messagingTemplate,
                fantasyDraftService,
                draftScheduler
        );
    }

    @Test
    void testBenchmarkGetDashboardGames() {
        // Setup data
        int gameCount = 100;
        int participantsPerGame = 50;
        List<FantasyGame> games = new ArrayList<>();
        List<FantasyParticipant> participants = new ArrayList<>();

        for (int i = 0; i < gameCount; i++) {
            FantasyGame game = FantasyGame.builder()
                    .title("Game " + i)
                    .status(FantasyGame.GameStatus.ONGOING)
                    .build();
            games.add(game);
        }
        fantasyGameRepository.saveAll(games);

        for (FantasyGame game : games) {
            for (int j = 0; j < participantsPerGame; j++) {
                FantasyParticipant participant = FantasyParticipant.builder()
                        .fantasyGameSeq(game.getSeq())
                        .playerId((long) (j + 1000))
                        .teamName("Team " + j)
                        .build();
                participants.add(participant);
            }
        }
        fantasyParticipantRepository.saveAll(participants);

        // Benchmark
        long userId = 1000L;
        int iterations = 10;
        long totalTime = 0;

        // Warmup
        fantasyGameService.getDashboardGames(userId);

        for (int i = 0; i < iterations; i++) {
            long start = System.currentTimeMillis();
            fantasyGameService.getDashboardGames(userId);
            long end = System.currentTimeMillis();
            totalTime += (end - start);
        }

        double averageTime = totalTime / (double) iterations;
        System.out.println("Average execution time for getDashboardGames: " + averageTime + " ms");
    }
}
