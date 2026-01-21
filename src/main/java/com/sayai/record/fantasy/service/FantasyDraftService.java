package com.sayai.record.fantasy.service;

import com.sayai.record.fantasy.dto.DraftEventDto;
import com.sayai.record.fantasy.dto.DraftRequest;
import com.sayai.record.fantasy.dto.FantasyPlayerDto;
import com.sayai.record.fantasy.entity.DraftPick;
import com.sayai.record.fantasy.entity.FantasyGame;
import com.sayai.record.fantasy.entity.FantasyParticipant;
import com.sayai.record.fantasy.entity.FantasyPlayer;
import com.sayai.record.fantasy.repository.DraftPickRepository;
import com.sayai.record.fantasy.repository.FantasyGameRepository;
import com.sayai.record.fantasy.repository.FantasyParticipantRepository;
import com.sayai.record.fantasy.repository.FantasyPlayerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
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
    private final FantasyParticipantRepository fantasyParticipantRepository;
    private final DraftValidator draftValidator;
    private final SimpMessagingTemplate messagingTemplate;

    @Transactional
    public void joinGame(Long gameSeq, Long playerId, String preferredTeam) {
        FantasyGame game = fantasyGameRepository.findById(gameSeq)
                .orElseThrow(() -> new IllegalArgumentException("Invalid Game Seq"));

        if (game.getStatus() != FantasyGame.GameStatus.WAITING) {
            throw new IllegalStateException("Cannot join game. Status is " + game.getStatus());
        }

        // Check if already joined
        if (fantasyParticipantRepository.findByFantasyGameSeqAndPlayerId(gameSeq, playerId).isPresent()) {
            throw new IllegalStateException("Player already joined this game");
        }

        FantasyParticipant participant = FantasyParticipant.builder()
                .fantasyGameSeq(gameSeq)
                .playerId(playerId)
                .preferredTeam(preferredTeam)
                .build();

        fantasyParticipantRepository.save(participant);
    }

    @Transactional(readOnly = true)
    public List<FantasyPlayerDto> getAvailablePlayers(Long gameSeq, String team, String position, String search) {
        // 1. Get all picks for this game
        List<DraftPick> picks = draftPickRepository.findByFantasyGameSeq(gameSeq);
        Set<Long> pickedPlayerSeqs = picks.stream()
                .map(DraftPick::getFantasyPlayerSeq)
                .collect(Collectors.toSet());

        // 2. Get filtered players from DB
        List<FantasyPlayer> filteredPlayers = fantasyPlayerRepository.findPlayers(team, position, search);

        return filteredPlayers.stream()
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

        // 3. Validate Rules
        FantasyPlayer targetPlayer = fantasyPlayerRepository.findById(request.getFantasyPlayerSeq())
                .orElseThrow(() -> new IllegalArgumentException("Invalid Player Seq"));

        // Get Current Picks for this user
        List<DraftPick> userPicks = draftPickRepository.findByFantasyGameSeqAndPlayerId(
                request.getFantasyGameSeq(), request.getPlayerId());

        Set<Long> pickedSeqs = userPicks.stream()
                .map(DraftPick::getFantasyPlayerSeq)
                .collect(Collectors.toSet());
        List<FantasyPlayer> currentTeam = fantasyPlayerRepository.findAllById(pickedSeqs);

        // Get Participant Info (needed for Rule 2)
        FantasyParticipant participant = fantasyParticipantRepository.findByFantasyGameSeqAndPlayerId(
                request.getFantasyGameSeq(), request.getPlayerId())
                .orElse(null); // Might be null if user didn't join explicitly (Rule 1 usually allows ad-hoc?)
                                // Actually better to require join for consistent logic, but let's handle null gracefully for Rule 1.

        draftValidator.validate(game, targetPlayer, currentTeam, participant);

        // 4. Save Pick
        // Calculate pick number
        long count = draftPickRepository.countByFantasyGameSeq(request.getFantasyGameSeq());
        int pickNumber = (int) count + 1;

        DraftPick pick = DraftPick.builder()
                .fantasyGameSeq(request.getFantasyGameSeq())
                .playerId(request.getPlayerId())
                .fantasyPlayerSeq(request.getFantasyPlayerSeq())
                .pickNumber(pickNumber)
                .build();

        draftPickRepository.save(pick);

        // Broadcast Event
        DraftEventDto event = DraftEventDto.builder()
                .type("PICK")
                .fantasyGameSeq(request.getFantasyGameSeq())
                .playerId(request.getPlayerId())
                .fantasyPlayerSeq(request.getFantasyPlayerSeq())
                .playerName(targetPlayer.getName())
                .playerTeam(targetPlayer.getTeam())
                .pickNumber(pickNumber)
                .message("Player " + request.getPlayerId() + " picked " + targetPlayer.getName() + " (Pick #" + pickNumber + ")")
                .build();

        messagingTemplate.convertAndSend("/topic/draft/" + request.getFantasyGameSeq(), event);
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
