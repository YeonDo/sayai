package com.sayai.record.fantasy.service;

import com.sayai.record.fantasy.dto.DraftEventDto;
import com.sayai.record.fantasy.dto.DraftRequest;
import com.sayai.record.fantasy.entity.*;
import com.sayai.record.fantasy.event.BotPickNeededEvent;
import com.sayai.record.fantasy.event.DraftFinishedEvent;
import com.sayai.record.fantasy.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class DraftPickExecutor {

    private final DraftPickRepository draftPickRepository;
    private final RosterLogRepository rosterLogRepository;
    private final FantasyParticipantRepository fantasyParticipantRepository;
    private final FantasyGameRepository fantasyGameRepository;
    private final SimpMessagingTemplate messagingTemplate;
    private final ApplicationEventPublisher eventPublisher;
    private final DraftValidator draftValidator;

    public static class NextPickInfo {
        public Long pickerId;
        public int round;
        public int pickInRound;
        public boolean isBot;
    }

    public NextPickInfo getNextPickInfo(FantasyGame game) {
        long totalPicks = draftPickRepository.countByFantasyGameSeq(game.getSeq());
        List<FantasyParticipant> participants = fantasyParticipantRepository.findByFantasyGameSeq(game.getSeq());
        participants.sort(Comparator.comparingInt(FantasyParticipant::getDraftOrder));
        int n = participants.size();
        if (n == 0) throw new IllegalStateException("No participants");

        int round = (int) (totalPicks / n) + 1;
        int index = (int) (totalPicks % n);

        int draftOrderIndex;
        if (round % 2 != 0) {
            draftOrderIndex = index + 1;
        } else {
            draftOrderIndex = n - index;
        }

        FantasyParticipant nextPicker = participants.get(draftOrderIndex - 1);

        NextPickInfo info = new NextPickInfo();
        info.pickerId = nextPicker.getMemberId();
        info.round = round;
        info.pickInRound = index + 1;
        info.isBot = Boolean.TRUE.equals(nextPicker.getIsBot());
        return info;
    }

    @Transactional
    public void commitPick(FantasyGame game, DraftRequest request, FantasyPlayer player,
                           boolean isDrafting, NextPickInfo currentPick) {
        if (draftPickRepository.existsByFantasyGameSeqAndFantasyPlayerSeq(
                request.getFantasyGameSeq(), request.getFantasyPlayerSeq())) {
            throw new IllegalStateException("이미 뽑힌 선수입니다");
        }

        long count = draftPickRepository.countByFantasyGameSeq(request.getFantasyGameSeq());
        int pickNumber = (int) count + 1;

        List<DraftPick> userPicks = draftPickRepository.findByFantasyGameSeqAndMemberId(
                request.getFantasyGameSeq(), request.getMemberId());
        String assignedPos = determineInitialPosition(userPicks, player);

        DraftPick pick = DraftPick.builder()
                .fantasyGameSeq(request.getFantasyGameSeq())
                .memberId(request.getMemberId())
                .fantasyPlayerSeq(request.getFantasyPlayerSeq())
                .pickNumber(pickNumber)
                .assignedPosition(assignedPos)
                .pickStatus(DraftPick.PickStatus.NORMAL)
                .build();
        draftPickRepository.save(pick);

        RosterLog.LogActionType actionType = isDrafting ? RosterLog.LogActionType.DRAFT_PICK : RosterLog.LogActionType.FA_ADD;
        String logDetails = isDrafting
                ? "Draft Pick #" + pickNumber + (request.isAutoPick() ? " (Auto)" : "")
                : player.getName() + " - Signed via FA";

        RosterLog logEntry = RosterLog.builder()
                .fantasyGameSeq(request.getFantasyGameSeq())
                .participantId(request.getMemberId())
                .fantasyPlayerSeq(request.getFantasyPlayerSeq())
                .actionType(actionType)
                .details(logDetails)
                .build();
        rosterLogRepository.save(logEntry);

        if (!isDrafting) {
            return;
        }

        NextPickInfo nextNext = getNextPickInfo(game);

        long totalPicks = draftPickRepository.countByFantasyGameSeq(request.getFantasyGameSeq());
        List<FantasyParticipant> participants = fantasyParticipantRepository.findByFantasyGameSeq(request.getFantasyGameSeq());
        int totalPlayersPerParticipant = draftValidator.getTotalPlayerCount(game.getRuleType());

        boolean isFinished = !participants.isEmpty() && totalPicks >= participants.size() * (long) totalPlayersPerParticipant;

        if (isFinished) {
            game.setStatus(FantasyGame.GameStatus.ONGOING);
            game.setNextPickDeadline(null);
        } else if (game.getDraftTimeLimit() != null && game.getDraftTimeLimit() > 0) {
            if (nextNext.isBot) {
                game.setNextPickDeadline(LocalDateTime.now().plusSeconds(5));
            } else {
                game.setNextPickDeadline(LocalDateTime.now().plusMinutes(game.getDraftTimeLimit()));
            }
        }
        // Always save explicitly: game may be a detached entity when called from the scheduler path
        // (autoPickAsync → same-object calls bypass @Transactional, so game is loaded outside a transaction)
        fantasyGameRepository.save(game);

        DraftEventDto event = DraftEventDto.builder()
                .type(isFinished ? "FINISH" : "PICK")
                .fantasyGameSeq(request.getFantasyGameSeq())
                .memberId(request.getMemberId())
                .fantasyPlayerSeq(request.getFantasyPlayerSeq())
                .playerName(player.getName())
                .playerTeam(player.getTeam())
                .pickNumber(pickNumber)
                .message(isFinished ? "Draft Completed!" : "Player " + request.getMemberId() + " picked " + player.getName() + " (Pick #" + pickNumber + ")")
                .isBot(currentPick != null && currentPick.isBot)
                .nextPickerIsBot(isFinished ? null : nextNext.isBot)
                .nextPickerId(isFinished ? null : nextNext.pickerId)
                .nextPickDeadline(game.getNextPickDeadline() != null ? game.getNextPickDeadline().atZone(ZoneId.of("UTC")) : null)
                .round(isFinished ? null : nextNext.round)
                .pickInRound(isFinished ? null : nextNext.pickInRound)
                .build();

        messagingTemplate.convertAndSend("/topic/draft/" + request.getFantasyGameSeq(), event);

        final long gameSeq = request.getFantasyGameSeq();
        if (isFinished) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    eventPublisher.publishEvent(new DraftFinishedEvent(gameSeq));
                }
            });
        } else if (nextNext.isBot) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    eventPublisher.publishEvent(new BotPickNeededEvent(gameSeq));
                }
            });
        }
    }

    public boolean hasOpenSlot(List<DraftPick> existingPicks, FantasyPlayer player) {
        return !"BENCH".equals(determineInitialPosition(existingPicks, player));
    }

    private String determineInitialPosition(List<DraftPick> existingPicks, FantasyPlayer newPlayer) {
        Map<String, Long> occupiedCounts = existingPicks.stream()
                .filter(p -> p.getAssignedPosition() != null)
                .collect(Collectors.groupingBy(DraftPick::getAssignedPosition, Collectors.counting()));

        String positionStr = newPlayer.getPosition() != null ? newPlayer.getPosition() : "";
        if (positionStr.trim().isEmpty()) {
            return "BENCH";
        }

        String[] positions = positionStr.split(",");
        String primaryPos = positions[0].trim();

        if (isPitcher(primaryPos)) {
            long spCount = occupiedCounts.getOrDefault("SP", 0L);
            long rpCount = occupiedCounts.getOrDefault("RP", 0L);
            long clCount = occupiedCounts.getOrDefault("CL", 0L);

            if (primaryPos.equals("SP")) return spCount < 4 ? "SP" : "BENCH";
            if (primaryPos.equals("RP")) return rpCount < 4 ? "RP" : "BENCH";
            if (primaryPos.equals("CL")) return clCount < 1 ? "CL" : "BENCH";
            return "BENCH";
        } else {
            for (String p : positions) {
                String pos = p.trim();
                if (!pos.isEmpty() && occupiedCounts.getOrDefault(pos, 0L) == 0) {
                    return pos;
                }
            }
            if (occupiedCounts.getOrDefault("DH", 0L) == 0) {
                return "DH";
            }
            return "BENCH";
        }
    }

    private boolean isPitcher(String pos) {
        return pos.equals("SP") || pos.equals("RP") || pos.equals("CL");
    }
}
