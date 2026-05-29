package com.sayai.record.fantasy.service;

import com.sayai.record.fantasy.entity.FantasyGame;
import com.sayai.record.fantasy.event.DraftFinishedEvent;
import com.sayai.record.fantasy.repository.FantasyGameRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class DraftScheduler {

    private final FantasyGameRepository fantasyGameRepository;
    private final DraftPickExecutor draftPickExecutor;
    private final DraftAutoPickService draftAutoPickService;

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

    @EventListener
    public void onDraftFinished(DraftFinishedEvent event) {
        removeActiveGame(event.gameSeq());
    }

    @Scheduled(fixedRate = 10000)
    public void checkDraftTimeouts() {
        if (activeGameSeqs.isEmpty()) {
            return;
        }

        List<FantasyGame> expiredGames = fantasyGameRepository.findExpiredDraftingGames(
                FantasyGame.GameStatus.DRAFTING, LocalDateTime.now());

        for (FantasyGame game : expiredGames) {
            draftAutoPickService.autoPickAsync(game.getSeq());
        }
    }
}
