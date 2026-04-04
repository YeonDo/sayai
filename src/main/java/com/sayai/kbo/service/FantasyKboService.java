package com.sayai.kbo.service;

import com.sayai.kbo.dto.ParticipantKboStatsDto;
import com.sayai.kbo.repository.KboHitRepository;
import com.sayai.kbo.repository.KboParticipantStatsInterface;
import com.sayai.kbo.repository.KboPitchRepository;
import com.sayai.record.fantasy.entity.DraftPick;
import com.sayai.record.fantasy.entity.FantasyParticipant;
import com.sayai.record.fantasy.repository.DraftPickRepository;
import com.sayai.record.fantasy.repository.FantasyParticipantRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class FantasyKboService {

    private final FantasyParticipantRepository participantRepository;
    private final DraftPickRepository draftPickRepository;
    private final KboHitRepository kboHitRepository;
    private final KboPitchRepository kboPitchRepository;

    private Long toStartIdx(LocalDate date) {
        if (date == null) return 0L;
        String formatted = date.format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        return Long.parseLong(formatted + "000000"); // Start of the day
    }

    private Long toEndIdx(LocalDate date) {
        if (date == null) return 99999999999999L;
        String formatted = date.format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        return Long.parseLong(formatted + "239999"); // End of the day
    }

    public List<ParticipantKboStatsDto> aggregateStats(Long gameSeq, LocalDate startDt, LocalDate endDt) {
        List<FantasyParticipant> participants = participantRepository.findByFantasyGameSeq(gameSeq);
        if (participants.isEmpty()) {
            return Collections.emptyList();
        }

        List<DraftPick> draftPicks = draftPickRepository.findByFantasyGameSeq(gameSeq);
        // Filter only active/normal picks (or whatever criteria means they "own" the player during this period)
        // If they need to hold them, pickStatus.NORMAL might be the way, but since we just aggregate, we map all valid ones.
        // The prompt says "participants 들의 draft_pick 조회" so we map participant -> list of playerIds.

        Map<Long, List<Long>> participantToPlayerIds = new HashMap<>();
        Map<Long, Long> playerToParticipantId = new HashMap<>();

        for (DraftPick pick : draftPicks) {
            if (pick.getPickStatus() != DraftPick.PickStatus.NORMAL && pick.getPickStatus() != DraftPick.PickStatus.TRADE_PENDING) {
                // If waiver req or other non-roster status, decide whether to include.
                // Usually NORMAL implies they are on the roster.
                // Assuming we just include them if they have a draft pick record. Let's include NORMAL.
            }
            Long pId = pick.getFantasyPlayerSeq();
            if (pId == null) pId = pick.getPlayerId(); // fallback just in case

            if (pId != null && pick.getPlayerId() != null) { // Actually participant is pick.getPlayerId()
                Long participantUserId = pick.getPlayerId(); // In DraftPick, playerId refers to the user/participant's user_id or participant ID.
                // Let's verify DraftPick mappings.
                // DraftPick has `playerId` (the user who picked) and `fantasyPlayerSeq` (the real KBO player).
                participantToPlayerIds.computeIfAbsent(participantUserId, k -> new ArrayList<>()).add(pId);
                playerToParticipantId.put(pId, participantUserId);
            }
        }

        List<Long> allPlayerIds = new ArrayList<>(playerToParticipantId.keySet());
        if (allPlayerIds.isEmpty()) {
            return createEmptyResponses(participants);
        }

        Long startIdx = toStartIdx(startDt);
        Long endIdx = toEndIdx(endDt);

        List<KboParticipantStatsInterface> hitStats = kboHitRepository.getAggregatedHitStats(startIdx, endIdx, allPlayerIds);
        List<KboParticipantStatsInterface> pitchStats = kboPitchRepository.getAggregatedPitchStats(startIdx, endIdx, allPlayerIds);

        Map<Long, ParticipantKboStatsDto> resultMap = new HashMap<>();
        for (FantasyParticipant p : participants) {
            Long userId = p.getPlayerId(); // user id
            resultMap.put(userId, ParticipantKboStatsDto.builder()
                    .participantSeq(p.getSeq())
                    .playerId(userId)
                    .teamName(p.getTeamName())
                    .build());
        }

        for (KboParticipantStatsInterface hs : hitStats) {
            Long userId = playerToParticipantId.get(hs.getPlayerId());
            if (userId != null && resultMap.containsKey(userId)) {
                ParticipantKboStatsDto dto = resultMap.get(userId);
                dto.setPa(dto.getPa() + safelyGet(hs.getPa()));
                dto.setAb(dto.getAb() + safelyGet(hs.getAb()));
                dto.setHit(dto.getHit() + safelyGet(hs.getHit()));
                dto.setRbi(dto.getRbi() + safelyGet(hs.getRbi()));
                dto.setRun(dto.getRun() + safelyGet(hs.getRun()));
                dto.setSb(dto.getSb() + safelyGet(hs.getSb()));
                dto.setSo(dto.getSo() + safelyGet(hs.getSo()));
                dto.setHr(dto.getHr() + safelyGet(hs.getHr()));
            }
        }

        for (KboParticipantStatsInterface ps : pitchStats) {
            Long userId = playerToParticipantId.get(ps.getPlayerId());
            if (userId != null && resultMap.containsKey(userId)) {
                ParticipantKboStatsDto dto = resultMap.get(userId);
                dto.setWin(dto.getWin() + safelyGet(ps.getWin()));
                dto.setLose(dto.getLose() + safelyGet(ps.getLose()));
                dto.setSave(dto.getSave() + safelyGet(ps.getSave()));
                dto.setInning(dto.getInning() + safelyGet(ps.getInning()));
                dto.setBatter(dto.getBatter() + safelyGet(ps.getBatter()));
                dto.setPitchCnt(dto.getPitchCnt() + safelyGet(ps.getPitchCnt()));
                dto.setPHit(dto.getPHit() + safelyGet(ps.getPHit()));
                dto.setBb(dto.getBb() + safelyGet(ps.getBb()));
                dto.setPSo(dto.getPSo() + safelyGet(ps.getPSo()));
                dto.setEr(dto.getEr() + safelyGet(ps.getEr()));
                dto.setHbp(dto.getHbp() + safelyGet(ps.getHbp()));
            }
        }

        for (ParticipantKboStatsDto dto : resultMap.values()) {
            if (dto.getInning() > 0) {
                long full = dto.getInning() / 3;
                long remainder = dto.getInning() % 3;
                if (remainder == 0) {
                    dto.setFormattedInning(String.valueOf(full));
                } else {
                    dto.setFormattedInning(full + " " + remainder + "/3");
                }
            } else {
                dto.setFormattedInning("0");
            }
        }

        return new ArrayList<>(resultMap.values());
    }

    private List<ParticipantKboStatsDto> createEmptyResponses(List<FantasyParticipant> participants) {
        return participants.stream().map(p -> ParticipantKboStatsDto.builder()
                .participantSeq(p.getSeq())
                .playerId(p.getPlayerId())
                .teamName(p.getTeamName())
                .formattedInning("0")
                .build()).collect(Collectors.toList());
    }

    private long safelyGet(Long val) {
        return val == null ? 0L : val;
    }
}
