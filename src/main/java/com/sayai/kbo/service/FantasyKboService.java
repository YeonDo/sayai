package com.sayai.kbo.service;

import com.sayai.kbo.dto.ParticipantKboStatsDto;
import com.sayai.kbo.repository.KboHitRepository;
import com.sayai.kbo.repository.KboParticipantStatsInterface;
import com.sayai.kbo.repository.KboPitchRepository;
import com.sayai.record.fantasy.entity.FantasyParticipant;
import com.sayai.record.fantasy.entity.DraftPickSnapshot;
import com.sayai.record.fantasy.repository.DraftPickSnapshotRepository;
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
    private final DraftPickSnapshotRepository draftPickSnapshotRepository;
    private final KboHitRepository kboHitRepository;
    private final KboPitchRepository kboPitchRepository;

    private static final Set<String> PITCHER_POSITIONS = Set.of("SP", "RP", "CL");
    private static final int REQUIRED_BATTERS = 9;
    private static final int REQUIRED_PITCHERS = 9;
    private static final int MIN_PA = 20;
    private static final int MIN_INNINGS = 15;

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

        List<DraftPickSnapshot> draftPicks = draftPickSnapshotRepository.findByFantasyGameSeq(gameSeq);

        Map<Long, List<Long>> participantToPlayerIds = new HashMap<>();
        Map<Long, Long> playerToParticipantId = new HashMap<>();
        Map<Long, Integer> batterCountByParticipant = new HashMap<>();
        Map<Long, Integer> pitcherCountByParticipant = new HashMap<>();

        for (DraftPickSnapshot pick : draftPicks) {
            Long pId = pick.getFantasyPlayerSeq();
            if (pId == null) pId = pick.getPlayerId();

            if (pId != null && pick.getPlayerId() != null) {
                Long participantUserId = pick.getPlayerId();
                participantToPlayerIds.computeIfAbsent(participantUserId, k -> new ArrayList<>()).add(pId);
                playerToParticipantId.put(pId, participantUserId);

                String pos = pick.getAssignedPosition();
                if (PITCHER_POSITIONS.contains(pos)) {
                    pitcherCountByParticipant.merge(participantUserId, 1, Integer::sum);
                } else {
                    batterCountByParticipant.merge(participantUserId, 1, Integer::sum);
                }
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

        for (FantasyParticipant p : participants) {
            Long userId = p.getPlayerId();
            ParticipantKboStatsDto dto = resultMap.get(userId);
            if (dto == null) continue;

            int missingBatters = Math.max(0, REQUIRED_BATTERS - batterCountByParticipant.getOrDefault(userId, 0));
            int missingPitchers = Math.max(0, REQUIRED_PITCHERS - pitcherCountByParticipant.getOrDefault(userId, 0));

            // Ghost batter: 12 AB, 12 SO, 0 H, 0 SB, 0 RBI
            if (missingBatters > 0) {
                dto.setAb(dto.getAb() + (long) missingBatters * 12);
                dto.setSo(dto.getSo() + (long) missingBatters * 12);
            }

            // Ghost pitcher: 9 ER, 9 pHit, 0 inning, 0 SO, 0 BB
            if (missingPitchers > 0) {
                dto.setEr(dto.getEr() + (long) missingPitchers * 9);
                dto.setPHit(dto.getPHit() + (long) missingPitchers * 9);
            }

            // Minimum PA: 1 missing PA → +1 AB, +1 SO
            long missingPa = Math.max(0L, MIN_PA - dto.getPa());
            if (missingPa > 0) {
                dto.setPa(dto.getPa() + missingPa);
                dto.setAb(dto.getAb() + missingPa);
                dto.setSo(dto.getSo() + missingPa);
            }

            // Minimum innings (stored as outs): 1 missing inning → +1 ER, +1 pHit
            long missingInnings = Math.max(0L, MIN_INNINGS - dto.getInning() / 3);
            if (missingInnings > 0) {
                dto.setEr(dto.getEr() + missingInnings);
                dto.setPHit(dto.getPHit() + missingInnings);
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
