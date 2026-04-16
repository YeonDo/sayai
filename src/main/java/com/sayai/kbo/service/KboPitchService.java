package com.sayai.kbo.service;

import com.sayai.record.dto.PitcherDto;
import com.sayai.kbo.repository.KboPitchRepository;
import com.sayai.kbo.repository.KboPitchStatInterface;
import com.sayai.kbo.repository.KboPitcherSeasonStatsProjection;
import com.sayai.kbo.repository.KboPitcherStatsRepository;
import lombok.AllArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.NoSuchElementException;

@Service
@AllArgsConstructor
@Transactional(readOnly = true)
public class KboPitchService {

    private final KboPitchRepository kboPitchRepository;
    private final KboPitcherStatsRepository kboPitcherStatsRepository;

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

    public Page<PitcherDto> selectBySeason(int season, Integer minOuts, Pageable pageable) {
        Page<KboPitcherSeasonStatsProjection> stats = kboPitcherStatsRepository.findBySeasonWithPlayerInfo(season, minOuts, pageable);
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

    public Page<PitcherDto> select(LocalDate startDate, LocalDate endDate, Pageable pageable) {
        Long startIdx = toStartIdx(startDate);
        Long endIdx = toEndIdx(endDate);
        Page<KboPitchStatInterface> stats = kboPitchRepository.getStatsByPeriod(startIdx, endIdx, pageable);
        return stats.map(this::mapToDto);
    }

    public PitcherDto selectOne(LocalDate startDate, LocalDate endDate, Long id) {
        try {
            Long startIdx = toStartIdx(startDate);
            Long endIdx = toEndIdx(endDate);
            KboPitchStatInterface stat = kboPitchRepository.getStatsByPeriodAndId(startIdx, endIdx, id).get();
            return mapToDto(stat);
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
        dto.setK9(null);

        return dto;
    }
}
