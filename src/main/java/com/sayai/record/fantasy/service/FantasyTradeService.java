package com.sayai.record.fantasy.service;

import com.sayai.record.fantasy.entity.DraftPick;
import com.sayai.record.fantasy.entity.FantasyGame;
import com.sayai.record.fantasy.entity.FantasyParticipant;
import com.sayai.record.fantasy.entity.FantasyPlayer;
import com.sayai.record.fantasy.repository.DraftPickRepository;
import com.sayai.record.fantasy.repository.FantasyGameRepository;
import com.sayai.record.fantasy.repository.FantasyParticipantRepository;
import com.sayai.record.fantasy.repository.FantasyPlayerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class FantasyTradeService {

    private final DraftPickRepository draftPickRepository;
    private final FantasyGameRepository fantasyGameRepository;
    private final FantasyPlayerRepository fantasyPlayerRepository;
    private final FantasyParticipantRepository fantasyParticipantRepository;

    @Transactional
    public void dropPlayer(Long gameSeq, Long playerId, Long fantasyPlayerSeq) {
        // 1. Check Game Status
        FantasyGame game = fantasyGameRepository.findById(gameSeq)
                .orElseThrow(() -> new IllegalArgumentException("Invalid Game Seq"));

        if (game.getStatus() != FantasyGame.GameStatus.ONGOING) {
            throw new IllegalStateException("선수를 방출할 수 없습니다. 게임이 진행중이 아닙니다.");
        }

        // 2. Check Roster Size
        long rosterSize = draftPickRepository.countByFantasyGameSeqAndPlayerId(gameSeq, playerId);
        if (rosterSize < 19) {
            throw new IllegalStateException("선수단 규모가 19명 미만인 경우 방출할 수 없습니다.");
        }

        // 3. Find Draft Pick
        DraftPick pick = draftPickRepository.findByFantasyGameSeqAndPlayerIdAndFantasyPlayerSeq(gameSeq, playerId, fantasyPlayerSeq)
                .orElseThrow(() -> new IllegalArgumentException("보유하지 않은 선수입니다."));

        // 4. Check Bench Status
        String pos = pick.getAssignedPosition();
        if (pos != null && !pos.equalsIgnoreCase("BENCH")) {
            throw new IllegalStateException("벤치 멤버만 방출할 수 있습니다.");
        }

        // 5. Drop (Delete Pick)
        draftPickRepository.delete(pick);
    }

    @Transactional
    public void claimPlayer(Long gameSeq, Long playerId, Long fantasyPlayerSeq) {
        // 1. Check Game Status
        FantasyGame game = fantasyGameRepository.findById(gameSeq)
                .orElseThrow(() -> new IllegalArgumentException("Invalid Game Seq"));

        if (game.getStatus() != FantasyGame.GameStatus.ONGOING) {
            throw new IllegalStateException("Cannot claim player. Game is not ONGOING.");
        }

        // 2. Check Availability
        if (draftPickRepository.existsByFantasyGameSeqAndFantasyPlayerSeq(gameSeq, fantasyPlayerSeq)) {
            throw new IllegalStateException("Player is already picked by another team.");
        }

        FantasyPlayer player = fantasyPlayerRepository.findById(fantasyPlayerSeq)
                .orElseThrow(() -> new IllegalArgumentException("Player not found"));

        // 3. Check Salary Cap & Roster Size
        long currentRosterSize = draftPickRepository.countByFantasyGameSeqAndPlayerId(gameSeq, playerId);
        List<DraftPick> myPicks = draftPickRepository.findByFantasyGameSeqAndPlayerId(gameSeq, playerId);

        // Calculate Current Cost
        Set<Long> myPickSeqs = myPicks.stream().map(DraftPick::getFantasyPlayerSeq).collect(Collectors.toSet());
        int currentCost = 0;
        if (!myPickSeqs.isEmpty()) {
            List<FantasyPlayer> myPlayers = fantasyPlayerRepository.findAllById(myPickSeqs);
            currentCost = myPlayers.stream().mapToInt(p -> p.getCost() == null ? 0 : p.getCost()).sum();
        }

        int playerCost = player.getCost() == null ? 0 : player.getCost();

        // Penalty Check (If size becomes 21)
        if (currentRosterSize == 20) {
            playerCost += 5;
        }

        if (game.getSalaryCap() != null && game.getSalaryCap() > 0) {
            if (currentCost + playerCost > game.getSalaryCap()) {
                throw new IllegalStateException("Salary Cap Exceeded. Cost: " + (currentCost + playerCost) + " / " + game.getSalaryCap());
            }
        }

        // 4. Update Waiver Order
        List<FantasyParticipant> participants = fantasyParticipantRepository.findByFantasyGameSeq(gameSeq);
        // Sort by waiver order asc (1 is top priority)
        participants.sort(Comparator.comparingInt(p -> p.getWaiverOrder() == null ? 999 : p.getWaiverOrder()));

        FantasyParticipant me = participants.stream()
                .filter(p -> p.getPlayerId().equals(playerId))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Participant not found"));

        int myOldOrder = me.getWaiverOrder();
        int maxOrder = participants.size();

        // Shift others up: If their order > myOldOrder, decrement by 1
        for (FantasyParticipant p : participants) {
            if (p.getWaiverOrder() > myOldOrder) {
                p.setWaiverOrder(p.getWaiverOrder() - 1);
            }
        }
        // Move me to last
        me.setWaiverOrder(maxOrder);

        fantasyParticipantRepository.saveAll(participants);

        // 5. Create Pick
        // For claimed player, initial position is BENCH unless we want to auto-assign?
        // Let's set to BENCH safely.
        DraftPick newPick = DraftPick.builder()
                .fantasyGameSeq(gameSeq)
                .playerId(playerId)
                .fantasyPlayerSeq(fantasyPlayerSeq)
                .pickNumber(0) // 0 or separate counter for FA? Using 0 to distinguish from draft picks might be okay or just irrelevant.
                .assignedPosition("BENCH")
                .pickedAt(LocalDateTime.now())
                .build();

        draftPickRepository.save(newPick);
    }
}
