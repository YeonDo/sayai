package com.sayai.kbo.service;

import com.sayai.record.dto.PitcherDto;
import com.sayai.kbo.repository.KboPitchRepository;
import com.sayai.kbo.repository.KboPitchStatInterface;
import lombok.AllArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.NoSuchElementException;

@Service
@AllArgsConstructor
@Transactional(readOnly = true)
public class KboPitchService {

    private final KboPitchRepository kboPitchRepository;

    public Page<PitcherDto> select(LocalDate startDate, LocalDate endDate, Pageable pageable) {
        Page<KboPitchStatInterface> stats = kboPitchRepository.getStatsByPeriod(startDate, endDate, pageable);
        return stats.map(this::mapToDto);
    }

    public PitcherDto selectOne(LocalDate startDate, LocalDate endDate, Long id) {
        try {
            KboPitchStatInterface stat = kboPitchRepository.getStatsByPeriodAndId(startDate, endDate, id).get();
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
                .wins(null)
                .loses(null)
                .saves(null)
                .batter(stat.getBatter())
                .baseOnBall(stat.getBaseOnBall())
                .hitByBall(stat.getHitByBall())
                .pHit(stat.getPHit())
                .selfLossScore(stat.getSelfLossScore())
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
        dto.setStOut(null);
        dto.setFallingBall(null);
        dto.setBalk(null);
        dto.setLossScore(null);
        dto.setBattingAvg(null);
        dto.setK9(null);

        return dto;
    }
}
