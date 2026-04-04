package com.sayai.kbo.service;

import com.sayai.record.dto.PlayerDto;
import com.sayai.kbo.repository.KboHitRepository;
import com.sayai.kbo.repository.KboHitStatInterface;
import lombok.AllArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

@Service
@AllArgsConstructor
@Transactional(readOnly = true)
public class KboHitService {

    private final KboHitRepository kboHitRepository;

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

    public PlayerDto findOne(LocalDate startDate, LocalDate endDate, Long id) {
        try {
            Long startIdx = toStartIdx(startDate);
            Long endIdx = toEndIdx(endDate);
            KboHitStatInterface stat = kboHitRepository.getPlayerByPeriodAndId(startIdx, endIdx, id)
                    .orElseThrow(java.util.NoSuchElementException::new);
            return mapToDto(stat);
        } catch (java.util.NoSuchElementException e) {
            return PlayerDto.builder().build();
        }
    }

    public Page<PlayerDto> findAllByPeriod(LocalDate startDate, LocalDate endDate, Pageable pageable) {
        Long startIdx = toStartIdx(startDate);
        Long endIdx = toEndIdx(endDate);
        Page<KboHitStatInterface> stats = kboHitRepository.getPlayerByPeriod(startIdx, endIdx, pageable);
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
}
