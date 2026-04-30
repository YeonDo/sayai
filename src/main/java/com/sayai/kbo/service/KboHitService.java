package com.sayai.kbo.service;

import com.sayai.kbo.dto.HitterDailyStatDto;
import com.sayai.kbo.dto.HitterDetailResponse;
import com.sayai.record.dto.PlayerDto;
import com.sayai.kbo.repository.KboHitRepository;
import com.sayai.kbo.repository.KboHitStatInterface;
import com.sayai.kbo.repository.KboHitterDailyStatInterface;
import com.sayai.kbo.repository.KboHitterSeasonStatsProjection;
import com.sayai.kbo.repository.KboHitterStatsRepository;
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

@Service
@AllArgsConstructor
@Transactional(readOnly = true)
public class KboHitService {

    private final KboHitRepository kboHitRepository;
    private final KboHitterStatsRepository kboHitterStatsRepository;

    private String resolvePositionPattern(String position) {
        if (position == null) return null;
        List<String> positions = switch (position.toUpperCase()) {
            case "IF" -> List.of("1B", "2B", "3B", "SS");
            case "OF" -> List.of("LF", "CF", "RF");
            default -> List.of(position.toUpperCase());
        };
        return "(^|,)(" + String.join("|", positions) + ")(,|$)";
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

    public HitterDetailResponse findOneWithDailyStats(LocalDate startDate, LocalDate endDate, Long id, Pageable pageable) {
        PlayerDto summary = findOne(startDate, endDate, id);
        Long startIdx = toStartIdx(startDate);
        Long endIdx = toEndIdx(endDate);
        Page<HitterDailyStatDto> dailyStats = kboHitRepository
                .getDailyStatsByPlayerId(id, startIdx, endIdx, pageable)
                .map(this::mapDailyToDto);
        return new HitterDetailResponse(summary, dailyStats);
    }

    public PlayerDto findOne(LocalDate startDate, LocalDate endDate, Long id) {
        try {
            Long startIdx = toStartIdx(startDate);
            Long endIdx = toEndIdx(endDate);
            KboHitStatInterface stat = kboHitRepository.getPlayerByPeriodAndId(startIdx, endIdx, id)
                    .orElseThrow(java.util.NoSuchElementException::new);
            return mapToDetailDto(stat);
        } catch (java.util.NoSuchElementException e) {
            return PlayerDto.builder().build();
        }
    }

    public Page<PlayerDto> findAllBySeason(int season, Integer minPa, String position, Pageable pageable) {
        String positionPattern = resolvePositionPattern(position);
        Page<KboHitterSeasonStatsProjection> stats = positionPattern != null
                ? kboHitterStatsRepository.findBySeasonWithPlayerInfoAndPositions(season, minPa, positionPattern, pageable)
                : kboHitterStatsRepository.findBySeasonWithPlayerInfo(season, minPa, pageable);
        return stats.map(this::mapSeasonToDto);
    }

    private PlayerDto mapSeasonToDto(KboHitterSeasonStatsProjection stat) {
        PlayerDto dto = PlayerDto.builder()
                .id(stat.getId())
                .name(stat.getName())
                .playerAppearance(stat.getPa() != null ? stat.getPa().longValue() : null)
                .atBat(stat.getAb() != null ? stat.getAb().longValue() : null)
                .totalHits(stat.getHit() != null ? stat.getHit().longValue() : null)
                .homeruns(stat.getHr() != null ? stat.getHr().longValue() : null)
                .rbi(stat.getRbi())
                .strikeOut(stat.getSo() != null ? stat.getSo().longValue() : null)
                .sb(stat.getSb())
                .build();

        dto.setTeam(stat.getTeam());

        if (stat.getAvg() != null) {
            try {
                dto.setBattingAvg(Double.parseDouble(stat.getAvg()));
            } catch (NumberFormatException ignored) {
                dto.setBattingAvg(0.0);
            }
        }
        return dto;
    }

    public Page<PlayerDto> findAllByPeriod(LocalDate startDate, LocalDate endDate, String position, Pageable pageable) {
        Long startIdx = toStartIdx(startDate);
        Long endIdx = toEndIdx(endDate);
        String positionPattern = resolvePositionPattern(position);
        Page<KboHitStatInterface> stats = positionPattern != null
                ? kboHitRepository.getPlayerByPeriodAndPositions(startIdx, endIdx, positionPattern, pageable)
                : kboHitRepository.getPlayerByPeriod(startIdx, endIdx, pageable);
        return stats.map(this::mapToDto);
    }

    private PlayerDto mapToDto(KboHitStatInterface stat) {
        PlayerDto dto = PlayerDto.builder()
                .id(stat.getId())
                .backNo(stat.getBackNo())
                .name(stat.getName())
                .totalGames(stat.getTotalGames())
                .playerAppearance(stat.getPlayerAppearance())
                .atBat(stat.getAtBat())
                .totalHits(stat.getTotalHits())
                .homeruns(stat.getHomeruns())
                .strikeOut(stat.getStrikeOut())
                .rbi(stat.getRbi())
                .runs(stat.getRuns())
                .sb(stat.getSb())
                .build();

        dto.setTeam(stat.getTeam());

        double battingAvg = 0.0;
        if (stat.getAtBat() != null && stat.getAtBat() > 0) {
            battingAvg = (double) stat.getTotalHits() / stat.getAtBat();
            battingAvg = Math.round(battingAvg * 1000.0) / 1000.0;
        }
        dto.setBattingAvg(battingAvg);

        dto.setAvgPa(null);
        dto.setOnBasePer(null);
        dto.setSlugPer(null);
        dto.setSingles(null);
        dto.setDoubles(null);
        dto.setTriples(null);
        dto.setBaseOnBall(null);
        dto.setHitByPitch(null);
        dto.setIbb(null);
        dto.setDp(null);
        dto.setSacrifice(null);
        dto.setSacFly(null);

        return dto;
    }

    private PlayerDto mapToDetailDto(KboHitStatInterface stat) {
        PlayerDto dto = mapToDto(stat);

        long pa = stat.getPlayerAppearance() != null ? stat.getPlayerAppearance() : 0L;
        long ab = stat.getAtBat() != null ? stat.getAtBat() : 0L;
        long so = stat.getStrikeOut() != null ? stat.getStrikeOut() : 0L;
        long bb = pa - ab;
        dto.setBb(bb);
        dto.setBbPerK(so > 0 ? Math.round((double) bb / so * 1000.0) / 1000.0 : 0.0);
        dto.setBbPct(pa > 0 ? Math.round((double) bb / pa * 1000.0) / 1000.0 : 0.0);
        dto.setKPct(pa > 0 ? Math.round((double) so / pa * 1000.0) / 1000.0 : 0.0);

        return dto;
    }

    private HitterDailyStatDto mapDailyToDto(KboHitterDailyStatInterface stat) {
        double battingAvg = 0.0;
        if (stat.getAb() != null && stat.getAb() > 0) {
            battingAvg = Math.round((double) stat.getHit() / stat.getAb() * 1000.0) / 1000.0;
        }
        long pa = stat.getPa() != null ? stat.getPa() : 0L;
        long ab = stat.getAb() != null ? stat.getAb() : 0L;
        long so = stat.getSo() != null ? stat.getSo() : 0L;
        long bb = pa - ab;
        return HitterDailyStatDto.builder()
                .gameDate(stat.getGameDate())
                .opponent(stat.getOpponent())
                .pa(stat.getPa())
                .ab(stat.getAb())
                .hit(stat.getHit())
                .hr(stat.getHr())
                .rbi(stat.getRbi())
                .run(stat.getRun())
                .sb(stat.getSb())
                .so(stat.getSo())
                .battingAvg(battingAvg)
                .bb(bb)
                .bbPerK(so > 0 ? Math.round((double) bb / so * 1000.0) / 1000.0 : 0.0)
                .bbPct(pa > 0 ? Math.round((double) bb / pa * 1000.0) / 1000.0 : 0.0)
                .kPct(pa > 0 ? Math.round((double) so / pa * 1000.0) / 1000.0 : 0.0)
                .build();
    }
}
