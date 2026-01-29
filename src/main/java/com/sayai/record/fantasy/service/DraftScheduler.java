package com.sayai.record.fantasy.service;

import com.sayai.record.fantasy.entity.FantasyGame;
import com.sayai.record.fantasy.repository.FantasyGameRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class DraftScheduler {

    private final FantasyGameRepository fantasyGameRepository;
    private final FantasyDraftService fantasyDraftService;

    @Scheduled(fixedRate = 10000) // Check every 10 seconds
    public void checkDraftTimeouts() {
        // Find DRAFTING games with deadline < NOW and timeLimit > 0
        List<FantasyGame> draftingGames = fantasyGameRepository.findExpiredDraftingGames(FantasyGame.GameStatus.DRAFTING, LocalDateTime.now());

        for (FantasyGame game : draftingGames) {
            try {
                // Double check status in case it changed
                fantasyDraftService.autoPick(game.getSeq());
            } catch (Exception e) {
                log.error("Error in autoPick for game {}: {}", game.getSeq(), e.getMessage());
            }
        }
    }
}
