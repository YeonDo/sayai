package com.sayai.record.fantasy.service;

import com.sayai.record.fantasy.entity.DraftPick;
import com.sayai.record.fantasy.entity.FantasyGame;
import com.sayai.record.fantasy.repository.DraftPickRepository;
import com.sayai.record.fantasy.repository.FantasyGameRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class FantasyTradeService {

    private final DraftPickRepository draftPickRepository;
    private final FantasyGameRepository fantasyGameRepository;

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
}
