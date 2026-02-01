package com.sayai.record.fantasy.service;

import com.sayai.record.fantasy.entity.FantasyGame;
import com.sayai.record.fantasy.repository.FantasyGameRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class DraftScheduler {

    private final FantasyGameRepository fantasyGameRepository;
    private final FantasyDraftService fantasyDraftService;

    private final Set<Long> activeGameSeqs = ConcurrentHashMap.newKeySet();

    @PostConstruct
    public void init() {
        List<FantasyGame> draftingGames = fantasyGameRepository.findAllByStatus(FantasyGame.GameStatus.DRAFTING);
        activeGameSeqs.addAll(draftingGames.stream().map(FantasyGame::getSeq).collect(Collectors.toList()));
        log.info("DraftScheduler initialized with {} active drafting games.", activeGameSeqs.size());
    }

    public void addActiveGame(Long gameSeq) {
        activeGameSeqs.add(gameSeq);
    }

    public void removeActiveGame(Long gameSeq) {
        activeGameSeqs.remove(gameSeq);
    }

    @Scheduled(fixedRate = 10000) // Check every 10 seconds
    public void checkDraftTimeouts() {
        if (activeGameSeqs.isEmpty()) {
            return;
        }

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
