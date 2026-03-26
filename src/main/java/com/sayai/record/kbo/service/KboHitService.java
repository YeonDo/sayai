package com.sayai.record.kbo.service;

import com.sayai.record.dto.PlayerDto;
import com.sayai.record.kbo.repository.KboHitRepository;
import com.sayai.record.kbo.repository.KboHitStatInterface;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Service
@AllArgsConstructor
@Transactional(readOnly = true)
public class KboHitService {

    private final KboHitRepository kboHitRepository;

    public PlayerDto findOne(LocalDate startDate, LocalDate endDate, Long id) {
        try {
            KboHitStatInterface stat = kboHitRepository.getPlayerByPeriodAndId(startDate, endDate, id)
                    .orElseThrow(java.util.NoSuchElementException::new);
            return mapToDto(stat);
        } catch (java.util.NoSuchElementException e) {
            return PlayerDto.builder().build();
        }
    }

    public List<PlayerDto> findAllByPeriod(LocalDate startDate, LocalDate endDate) {
        List<KboHitStatInterface> stats = kboHitRepository.getPlayerByPeriod(startDate, endDate);
        List<PlayerDto> result = new ArrayList<>();
        for (KboHitStatInterface stat : stats) {
            result.add(mapToDto(stat));
        }
        return result;
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

        // Calculate averages safely
        double battingAvg = 0.0;
        if (stat.getAtBat() != null && stat.getAtBat() > 0) {
            battingAvg = (double) stat.getTotalHits() / stat.getAtBat();
            // Round to 3 decimal places
            battingAvg = Math.round(battingAvg * 1000.0) / 1000.0;
        }
        dto.setBattingAvg(battingAvg);

        // Explicitly set null to fields we don't have
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
