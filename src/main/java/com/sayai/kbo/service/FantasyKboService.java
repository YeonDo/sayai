package com.sayai.kbo.service;

import com.sayai.kbo.dto.MyRosterStatDto;
import com.sayai.kbo.dto.ParticipantKboStatsDto;
import com.sayai.kbo.repository.KboHitRepository;
import com.sayai.kbo.repository.KboParticipantStatsInterface;
import com.sayai.kbo.repository.KboPitchRepository;
import com.sayai.record.fantasy.entity.DraftPick;
import com.sayai.record.fantasy.entity.DraftPickSnapshot;
import com.sayai.record.fantasy.entity.FantasyParticipant;
import com.sayai.record.fantasy.entity.FantasyPlayer;
import com.sayai.record.fantasy.repository.DraftPickRepository;
import com.sayai.record.fantasy.repository.DraftPickSnapshotRepository;
import com.sayai.record.fantasy.repository.FantasyParticipantRepository;
import com.sayai.record.fantasy.repository.FantasyPlayerRepository;
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
    private final DraftPickSnapshotRepository draftPickSnapshotRepository;
    private final KboHitRepository kboHitRepository;
    private final KboPitchRepository kboPitchRepository;
    private final FantasyPlayerRepository fantasyPlayerRepository;

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

        Map<Long, Long> playerToParticipantId = new HashMap<>();
        Map<Long, Integer> batterCountByParticipant = new HashMap<>();
        Map<Long, Integer> pitcherCountByParticipant = new HashMap<>();

        for (DraftPickSnapshot pick : draftPicks) {
            Long pId = pick.getFantasyPlayerSeq();
            if (pId == null) pId = pick.getMemberId();

            if (pId != null && pick.getMemberId() != null) {
                Long participantUserId = pick.getMemberId();
                playerToParticipantId.put(pId, participantUserId);

                if (PITCHER_POSITIONS.contains(pick.getAssignedPosition())) {
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
            Long userId = p.getMemberId();
            resultMap.put(userId, ParticipantKboStatsDto.builder()
                    .participantSeq(p.getSeq())
                    .memberId(userId)
                    .teamName(p.getTeamName())
                    .build());
        }

        for (KboParticipantStatsInterface hs : hitStats) {
            ParticipantKboStatsDto dto = resultMap.get(playerToParticipantId.get(hs.getPlayerId()));
            if (dto != null) {
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
            ParticipantKboStatsDto dto = resultMap.get(playerToParticipantId.get(ps.getPlayerId()));
            if (dto != null) {
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
            Long userId = p.getMemberId();
            ParticipantKboStatsDto dto = resultMap.get(userId);
            if (dto == null) continue;

            int missingBatters = Math.max(0, REQUIRED_BATTERS - batterCountByParticipant.getOrDefault(userId, 0));
            int missingPitchers = Math.max(0, REQUIRED_PITCHERS - pitcherCountByParticipant.getOrDefault(userId, 0));

            if (missingBatters > 0) {
                dto.setAb(dto.getAb() + (long) missingBatters * 12);
                dto.setSo(dto.getSo() + (long) missingBatters * 12);
            }

            if (missingPitchers > 0) {
                dto.setEr(dto.getEr() + (long) missingPitchers * 9);
                dto.setPHit(dto.getPHit() + (long) missingPitchers * 9);
            }

            long missingPa = Math.max(0L, MIN_PA - dto.getPa());
            if (missingPa > 0) {
                dto.setPa(dto.getPa() + missingPa);
                dto.setAb(dto.getAb() + missingPa);
                dto.setSo(dto.getSo() + missingPa);
            }

            long missingInnings = Math.max(0L, MIN_INNINGS - dto.getInning() / 3);
            if (missingInnings > 0) {
                dto.setEr(dto.getEr() + missingInnings);
                dto.setPHit(dto.getPHit() + missingInnings);
            }

            long full = dto.getInning() / 3;
            long remainder = dto.getInning() % 3;
            dto.setFormattedInning(dto.getInning() == 0 ? "0"
                    : remainder == 0 ? String.valueOf(full)
                    : full + " " + remainder + "/3");
        }

        return new ArrayList<>(resultMap.values());
    }

    private List<ParticipantKboStatsDto> createEmptyResponses(List<FantasyParticipant> participants) {
        return participants.stream().map(p -> ParticipantKboStatsDto.builder()
                .participantSeq(p.getSeq())
                .memberId(p.getMemberId())
                .teamName(p.getTeamName())
                .formattedInning("0")
                .build()).collect(Collectors.toList());
    }

    private long safelyGet(Long val) {
        return val == null ? 0L : val;
    }

    public MyRosterStatDto getMyRosterStats(Long gameSeq, Long playerId, LocalDate startDt, LocalDate endDt) {
        List<DraftPick> myPicks = draftPickRepository.findByFantasyGameSeqAndMemberId(gameSeq, playerId);

        if (myPicks.isEmpty()) {
            return MyRosterStatDto.builder()
                    .hitters(Collections.emptyList())
                    .pitchers(Collections.emptyList())
                    .hitterTotal(MyRosterStatDto.HitterStat.builder().avg(".000").build())
                    .pitcherTotal(MyRosterStatDto.PitcherStat.builder().formattedInning("0").era("-.--").whip("-.--").build())
                    .build();
        }

        List<Long> allSeqs = myPicks.stream()
                .map(DraftPick::getFantasyPlayerSeq)
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());

        Map<Long, FantasyPlayer> playerMap = fantasyPlayerRepository.findAllById(allSeqs)
                .stream().collect(Collectors.toMap(FantasyPlayer::getSeq, p -> p));

        Map<Long, String> assignedPosMap = myPicks.stream()
                .filter(p -> p.getFantasyPlayerSeq() != null)
                .collect(Collectors.toMap(
                        DraftPick::getFantasyPlayerSeq,
                        DraftPick::getAssignedPosition,
                        (a, b) -> a));

        List<Long> hitterSeqs = allSeqs.stream()
                .filter(seq -> {
                    FantasyPlayer fp = playerMap.get(seq);
                    return fp != null && !PITCHER_POSITIONS.contains(fp.getPosition());
                })
                .collect(Collectors.toList());

        List<Long> pitcherSeqs = allSeqs.stream()
                .filter(seq -> {
                    FantasyPlayer fp = playerMap.get(seq);
                    return fp != null && PITCHER_POSITIONS.contains(fp.getPosition());
                })
                .collect(Collectors.toList());

        Long startIdx = toStartIdx(startDt);
        Long endIdx = toEndIdx(endDt);

        Map<Long, KboParticipantStatsInterface> hitStatsMap = hitterSeqs.isEmpty()
                ? Collections.emptyMap()
                : kboHitRepository.getAggregatedHitStats(startIdx, endIdx, hitterSeqs)
                        .stream().collect(Collectors.toMap(KboParticipantStatsInterface::getPlayerId, s -> s));

        Map<Long, KboParticipantStatsInterface> pitchStatsMap = pitcherSeqs.isEmpty()
                ? Collections.emptyMap()
                : kboPitchRepository.getAggregatedPitchStats(startIdx, endIdx, pitcherSeqs)
                        .stream().collect(Collectors.toMap(KboParticipantStatsInterface::getPlayerId, s -> s));

        List<MyRosterStatDto.HitterStat> hitters = hitterSeqs.stream().map(seq -> {
            FantasyPlayer fp = playerMap.get(seq);
            KboParticipantStatsInterface s = hitStatsMap.get(seq);
            long ab = s != null ? safelyGet(s.getAb()) : 0L;
            long hit = s != null ? safelyGet(s.getHit()) : 0L;
            return MyRosterStatDto.HitterStat.builder()
                    .fantasyPlayerSeq(seq)
                    .playerName(fp != null ? fp.getName() : "Unknown")
                    .kboTeam(fp != null ? fp.getTeam() : "")
                    .position(fp != null ? fp.getPosition() : "")
                    .assignedPosition(assignedPosMap.getOrDefault(seq, ""))
                    .pa(s != null ? safelyGet(s.getPa()) : 0L)
                    .ab(ab)
                    .hit(hit)
                    .hr(s != null ? safelyGet(s.getHr()) : 0L)
                    .rbi(s != null ? safelyGet(s.getRbi()) : 0L)
                    .run(s != null ? safelyGet(s.getRun()) : 0L)
                    .sb(s != null ? safelyGet(s.getSb()) : 0L)
                    .so(s != null ? safelyGet(s.getSo()) : 0L)
                    .avg(ab == 0 ? ".000" : hit >= ab ? "1.000" : String.format(".%03d", (int) (hit * 1000 / ab)))
                    .build();
        }).collect(Collectors.toList());

        List<MyRosterStatDto.PitcherStat> pitchers = pitcherSeqs.stream().map(seq -> {
            FantasyPlayer fp = playerMap.get(seq);
            KboParticipantStatsInterface s = pitchStatsMap.get(seq);
            long inning = s != null ? safelyGet(s.getInning()) : 0L;
            long er = s != null ? safelyGet(s.getEr()) : 0L;
            long bb = s != null ? safelyGet(s.getBb()) : 0L;
            long pHit = s != null ? safelyGet(s.getPHit()) : 0L;
            return MyRosterStatDto.PitcherStat.builder()
                    .fantasyPlayerSeq(seq)
                    .playerName(fp != null ? fp.getName() : "Unknown")
                    .kboTeam(fp != null ? fp.getTeam() : "")
                    .position(fp != null ? fp.getPosition() : "")
                    .assignedPosition(assignedPosMap.getOrDefault(seq, ""))
                    .win(s != null ? safelyGet(s.getWin()) : 0L)
                    .lose(s != null ? safelyGet(s.getLose()) : 0L)
                    .save(s != null ? safelyGet(s.getSave()) : 0L)
                    .inning(inning)
                    .formattedInning(formatInning(inning))
                    .er(er)
                    .bb(bb)
                    .pHit(pHit)
                    .pSo(s != null ? safelyGet(s.getPSo()) : 0L)
                    .hbp(s != null ? safelyGet(s.getHbp()) : 0L)
                    .era(inning == 0 ? "-.--" : String.format("%.2f", (double) er * 27 / inning))
                    .whip(inning == 0 ? "-.--" : String.format("%.2f", (double) (bb + pHit) * 3 / inning))
                    .build();
        }).collect(Collectors.toList());

        return MyRosterStatDto.builder()
                .hitters(hitters)
                .pitchers(pitchers)
                .hitterTotal(buildHitterTotal(hitters))
                .pitcherTotal(buildPitcherTotal(pitchers))
                .build();
    }

    private String formatInning(long outs) {
        long full = outs / 3;
        long remainder = outs % 3;
        return outs == 0 ? "0" : remainder == 0 ? String.valueOf(full) : full + " " + remainder + "/3";
    }

    private MyRosterStatDto.HitterStat buildHitterTotal(List<MyRosterStatDto.HitterStat> hitters) {
        long ab = hitters.stream().mapToLong(MyRosterStatDto.HitterStat::getAb).sum();
        long hit = hitters.stream().mapToLong(MyRosterStatDto.HitterStat::getHit).sum();
        return MyRosterStatDto.HitterStat.builder()
                .pa(hitters.stream().mapToLong(MyRosterStatDto.HitterStat::getPa).sum())
                .ab(ab)
                .hit(hit)
                .hr(hitters.stream().mapToLong(MyRosterStatDto.HitterStat::getHr).sum())
                .rbi(hitters.stream().mapToLong(MyRosterStatDto.HitterStat::getRbi).sum())
                .run(hitters.stream().mapToLong(MyRosterStatDto.HitterStat::getRun).sum())
                .sb(hitters.stream().mapToLong(MyRosterStatDto.HitterStat::getSb).sum())
                .so(hitters.stream().mapToLong(MyRosterStatDto.HitterStat::getSo).sum())
                .avg(ab == 0 ? ".000" : hit >= ab ? "1.000" : String.format(".%03d", (int) (hit * 1000 / ab)))
                .build();
    }

    private MyRosterStatDto.PitcherStat buildPitcherTotal(List<MyRosterStatDto.PitcherStat> pitchers) {
        long inning = pitchers.stream().mapToLong(MyRosterStatDto.PitcherStat::getInning).sum();
        long er = pitchers.stream().mapToLong(MyRosterStatDto.PitcherStat::getEr).sum();
        long bb = pitchers.stream().mapToLong(MyRosterStatDto.PitcherStat::getBb).sum();
        long pHit = pitchers.stream().mapToLong(MyRosterStatDto.PitcherStat::getPHit).sum();
        return MyRosterStatDto.PitcherStat.builder()
                .win(pitchers.stream().mapToLong(MyRosterStatDto.PitcherStat::getWin).sum())
                .lose(pitchers.stream().mapToLong(MyRosterStatDto.PitcherStat::getLose).sum())
                .save(pitchers.stream().mapToLong(MyRosterStatDto.PitcherStat::getSave).sum())
                .inning(inning)
                .formattedInning(formatInning(inning))
                .er(er)
                .bb(bb)
                .pHit(pHit)
                .pSo(pitchers.stream().mapToLong(MyRosterStatDto.PitcherStat::getPSo).sum())
                .hbp(pitchers.stream().mapToLong(MyRosterStatDto.PitcherStat::getHbp).sum())
                .era(inning == 0 ? "-.--" : String.format("%.2f", (double) er * 27 / inning))
                .whip(inning == 0 ? "-.--" : String.format("%.2f", (double) (bb + pHit) * 3 / inning))
                .build();
    }
}
