package com.sayai.kbo.service;

import com.sayai.kbo.dto.PitcherDailyStatDto;
import com.sayai.kbo.dto.PitcherDetailResponse;
import com.sayai.record.dto.PitcherDto;
import com.sayai.kbo.repository.KboPitchRepository;
import com.sayai.kbo.repository.KboPitchStatInterface;
import com.sayai.kbo.repository.KboPitcherDailyStatInterface;
import com.sayai.kbo.repository.KboPitcherSeasonStatsProjection;
import com.sayai.kbo.repository.KboPitcherStatsRepository;
import lombok.AllArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.NoSuchElementException;

@Service
@AllArgsConstructor
@Transactional(readOnly = true)
public class KboPitchService {

    private final KboPitchRepository kboPitchRepository;
    private final KboPitcherStatsRepository kboPitcherStatsRepository;

    private String resolvePositionPattern(String position) {
        if (position == null) return null;
        return "(^|,)" + position.toUpperCase() + "(,|$)";
    }

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

    public Page<PitcherDto> selectBySeason(int season, Integer minOuts, String position, Pageable pageable) {
        String positionPattern = resolvePositionPattern(position);
        Page<KboPitcherSeasonStatsProjection> stats = positionPattern != null
                ? kboPitcherStatsRepository.findBySeasonWithPlayerInfoAndPositions(season, minOuts, positionPattern, pageable)
                : kboPitcherStatsRepository.findBySeasonWithPlayerInfo(season, minOuts, pageable);
        return stats.map(this::mapSeasonToDto);
    }

    private PitcherDto mapSeasonToDto(KboPitcherSeasonStatsProjection stat) {
        PitcherDto dto = PitcherDto.builder()
                .id(stat.getId())
                .name(stat.getName())
                .wins(stat.getWin() != null ? stat.getWin().longValue() : null)
                .saves(stat.getSave() != null ? stat.getSave().longValue() : null)
                .stOut(stat.getSo() != null ? stat.getSo().longValue() : null)
                .baseOnBall(stat.getBb() != null ? stat.getBb().longValue() : null)
                .pHit(stat.getPhit() != null ? stat.getPhit().longValue() : null)
                .selfLossScore(stat.getEr() != null ? stat.getEr().longValue() : null)
                .build();

        dto.setTeam(stat.getTeam());

        if (stat.getOuts() != null) {
            double inn = stat.getOuts() / 3 + (stat.getOuts() % 3) * 0.1;
            dto.setInnings(inn);
        }
        if (stat.getEra() != null) {
            try {
                dto.setEra(Double.parseDouble(stat.getEra()));
            } catch (NumberFormatException ignored) {
                dto.setEra(0.0);
            }
        }
        if (stat.getWhip() != null) {
            try {
                dto.setWhip(Double.parseDouble(stat.getWhip()));
            } catch (NumberFormatException ignored) {
                dto.setWhip(0.0);
            }
        }
        return dto;
    }

    public Page<PitcherDto> select(LocalDate startDate, LocalDate endDate, String position, Pageable pageable) {
        Long startIdx = toStartIdx(startDate);
        Long endIdx = toEndIdx(endDate);
        String positionPattern = resolvePositionPattern(position);
        Page<KboPitchStatInterface> stats = positionPattern != null
                ? kboPitchRepository.getStatsByPeriodAndPositions(startIdx, endIdx, positionPattern, pageable)
                : kboPitchRepository.getStatsByPeriod(startIdx, endIdx, pageable);
        return stats.map(this::mapToDto);
    }

    public PitcherDetailResponse selectOneWithDailyStats(LocalDate startDate, LocalDate endDate, Long id, Pageable pageable) {
        PitcherDto summary = selectOne(startDate, endDate, id);
        Long startIdx = toStartIdx(startDate);
        Long endIdx = toEndIdx(endDate);
        Page<PitcherDailyStatDto> dailyStats = kboPitchRepository
                .getDailyStatsByPlayerId(id, startIdx, endIdx, pageable)
                .map(this::mapDailyToDto);
        return new PitcherDetailResponse(summary, dailyStats);
    }

    public PitcherDto selectOne(LocalDate startDate, LocalDate endDate, Long id) {
        try {
            Long startIdx = toStartIdx(startDate);
            Long endIdx = toEndIdx(endDate);
            KboPitchStatInterface stat = kboPitchRepository.getStatsByPeriodAndId(startIdx, endIdx, id).get();
            return mapToDetailDto(stat);
        } catch (NoSuchElementException e) {
            return new PitcherDto();
        }
    }

    private PitcherDto mapToDto(KboPitchStatInterface stat) {
        PitcherDto dto = PitcherDto.builder()
                .id(stat.getId())
                .backNo(stat.getBackNo())
                .name(stat.getName())
                .wins(stat.getWins())
                .loses(stat.getLoses())
                .saves(stat.getSaves())
                .batter(stat.getBatter())
                .baseOnBall(stat.getBaseOnBall())
                .hitByBall(stat.getHitByBall())
                .pHit(stat.getPHit())
                .selfLossScore(stat.getSelfLossScore())
                .stOut(stat.getStOut())
                .build();

        dto.setTeam(stat.getTeam());

        Double inn = stat.getInning() / 3 + (stat.getInning() % 3) * 0.1;
        dto.setInnings(inn);
        dto.setInn(null); // Following prompt requirement

        Double era = 0.0;
        if (stat.getInning() != null && stat.getInning() > 0) {
            era = (stat.getSelfLossScore() * 27.0) / stat.getInning();
            era = Math.round(era * 100.0) / 100.0;
        }
        dto.setEra(era);

        Double whip = 0.0;
        if (stat.getInning() != null && stat.getInning() > 0) {
            whip = ((stat.getPHit() + stat.getBaseOnBall()) * 3.0) / stat.getInning();
            whip = Math.round(whip * 100.0) / 100.0;
        }
        dto.setWhip(whip);

        // Set missing fields to null
        dto.setHitter(null);
        dto.setPHomerun(null);
        dto.setSacrifice(null);
        dto.setSacFly(null);
        dto.setFallingBall(null);
        dto.setBalk(null);
        dto.setLossScore(null);
        dto.setBattingAvg(null);

        return dto;
    }

    private PitcherDto mapToDetailDto(KboPitchStatInterface stat) {
        long inning = stat.getInning() != null ? stat.getInning() : 0L;
        long so = stat.getStOut() != null ? stat.getStOut() : 0L;
        long bb = stat.getBaseOnBall() != null ? stat.getBaseOnBall() : 0L;
        long pitchCnt = stat.getPitchCnt() != null ? stat.getPitchCnt() : 0L;
        long batter = stat.getBatter() != null ? stat.getBatter() : 0L;
        long totalGames = stat.getTotalGames() != null ? stat.getTotalGames() : 0L;

        PitcherDto dto = PitcherDto.builder()
                .id(stat.getId())
                .backNo(stat.getBackNo())
                .name(stat.getName())
                .wins(stat.getWins())
                .loses(stat.getLoses())
                .saves(stat.getSaves())
                .batter(batter)
                .baseOnBall(bb)
                .hitByBall(stat.getHitByBall())
                .pHit(stat.getPHit())
                .selfLossScore(stat.getSelfLossScore())
                .stOut(so)
                .build();

        dto.setTeam(stat.getTeam());
        dto.setInnings(inning > 0 ? inning / 3 + (inning % 3) * 0.1 : 0.0);
        dto.setInn(null);
        dto.setEra(inning > 0 ? Math.round(stat.getSelfLossScore() * 27.0 / inning * 100.0) / 100.0 : 0.0);
        dto.setWhip(inning > 0 ? Math.round((stat.getPHit() + bb) * 3.0 / inning * 100.0) / 100.0 : 0.0);
        dto.setHitter(null);
        dto.setPHomerun(null);
        dto.setSacrifice(null);
        dto.setSacFly(null);
        dto.setFallingBall(null);
        dto.setBalk(null);
        dto.setLossScore(null);
        dto.setBattingAvg(null);

        dto.setPitchCnt(pitchCnt);
        dto.setK9(inning > 0 ? Math.round(so * 27.0 / inning * 100.0) / 100.0 : 0.0);
        dto.setBb9(inning > 0 ? Math.round(bb * 27.0 / inning * 100.0) / 100.0 : 0.0);
        dto.setKbb(bb > 0 ? Math.round((double) so / bb * 100.0) / 100.0 : 0.0);
        dto.setPg(totalGames > 0 ? Math.round((double) pitchCnt / totalGames * 10.0) / 10.0 : 0.0);
        dto.setPip(inning > 0 ? Math.round((double) pitchCnt * 3 / inning * 100.0) / 100.0 : 0.0);
        dto.setPpa(batter > 0 ? Math.round((double) pitchCnt / batter * 100.0) / 100.0 : 0.0);

        return dto;
    }

    private PitcherDailyStatDto mapDailyToDto(KboPitcherDailyStatInterface stat) {
        long inning = stat.getInning() != null ? stat.getInning() : 0L;
        long so = stat.getSo() != null ? stat.getSo() : 0L;
        long bb = stat.getBb() != null ? stat.getBb() : 0L;
        long pitchCnt = stat.getPitchCnt() != null ? stat.getPitchCnt() : 0L;
        long batter = stat.getBatter() != null ? stat.getBatter() : 0L;

        double innings = 0.0;
        double era = 0.0;
        if (inning > 0) {
            innings = inning / 3 + (inning % 3) * 0.1;
            era = Math.round((stat.getEr() * 27.0) / inning * 100.0) / 100.0;
        }
        return PitcherDailyStatDto.builder()
                .gameDate(stat.getGameDate())
                .opponent(stat.getOpponent())
                .innings(innings)
                .win(stat.getWin())
                .lose(stat.getLose())
                .save(stat.getSave())
                .er(stat.getEr())
                .bb(stat.getBb())
                .hbp(stat.getHbp())
                .pHit(stat.getPHit())
                .so(stat.getSo())
                .era(era)
                .pitchCnt(pitchCnt)
                .k9(inning > 0 ? Math.round(so * 27.0 / inning * 100.0) / 100.0 : 0.0)
                .bb9(inning > 0 ? Math.round(bb * 27.0 / inning * 100.0) / 100.0 : 0.0)
                .kbb(bb > 0 ? Math.round((double) so / bb * 100.0) / 100.0 : 0.0)
                .pip(inning > 0 ? Math.round((double) pitchCnt * 3 / inning * 100.0) / 100.0 : 0.0)
                .ppa(batter > 0 ? Math.round((double) pitchCnt / batter * 100.0) / 100.0 : 0.0)
                .build();
    }
}
