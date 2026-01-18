package com.sayai.record.fantasy.service;

import com.sayai.record.fantasy.dto.DraftRequest;
import com.sayai.record.fantasy.dto.FantasyPlayerDto;
import com.sayai.record.fantasy.entity.DraftPick;
import com.sayai.record.fantasy.entity.FantasyGame;
import com.sayai.record.fantasy.entity.FantasyPlayer;
import com.sayai.record.fantasy.repository.DraftPickRepository;
import com.sayai.record.fantasy.repository.FantasyGameRepository;
import com.sayai.record.fantasy.repository.FantasyPlayerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class FantasyDraftService {

    private final FantasyPlayerRepository fantasyPlayerRepository;
    private final DraftPickRepository draftPickRepository;
    private final FantasyGameRepository fantasyGameRepository;

    @Transactional(readOnly = true)
    public List<FantasyPlayerDto> getAvailablePlayers(Long gameSeq) {
        // 1. Get all picks for this game
        List<DraftPick> picks = draftPickRepository.findByFantasyGameSeq(gameSeq);
        Set<Long> pickedPlayerSeqs = picks.stream()
                .map(DraftPick::getFantasyPlayerSeq)
                .collect(Collectors.toSet());

        // 2. Get all players and filter
        List<FantasyPlayer> allPlayers = fantasyPlayerRepository.findAll();

        return allPlayers.stream()
                .filter(p -> !pickedPlayerSeqs.contains(p.getSeq()))
                .map(FantasyPlayerDto::from)
                .collect(Collectors.toList());
    }

    @Transactional
    public void draftPlayer(DraftRequest request) {
        // 1. Check Game Status
        FantasyGame game = fantasyGameRepository.findById(request.getFantasyGameSeq())
                .orElseThrow(() -> new IllegalArgumentException("Invalid Game Seq"));

        if (game.getStatus() != FantasyGame.GameStatus.DRAFTING) {
            throw new IllegalStateException("Game is not in DRAFTING status");
        }

        // 2. Check availability
        boolean isPicked = draftPickRepository.existsByFantasyGameSeqAndFantasyPlayerSeq(
                request.getFantasyGameSeq(),
                request.getFantasyPlayerSeq()
        );
        if (isPicked) {
            throw new IllegalStateException("Player already picked");
        }

        // 3. Save Pick
        DraftPick pick = DraftPick.builder()
                .fantasyGameSeq(request.getFantasyGameSeq())
                .playerId(request.getPlayerId())
                .fantasyPlayerSeq(request.getFantasyPlayerSeq())
                .build();

        draftPickRepository.save(pick);
    }

    @Transactional(readOnly = true)
    public List<FantasyPlayerDto> getPickedPlayers(Long gameSeq, Long playerId) {
        List<DraftPick> picks = draftPickRepository.findByFantasyGameSeqAndPlayerId(gameSeq, playerId);
        Set<Long> pickedSeqs = picks.stream()
                .map(DraftPick::getFantasyPlayerSeq)
                .collect(Collectors.toSet());

        List<FantasyPlayer> players = fantasyPlayerRepository.findAllById(pickedSeqs);

        return players.stream()
                .map(FantasyPlayerDto::from)
                .collect(Collectors.toList());
    }
}
