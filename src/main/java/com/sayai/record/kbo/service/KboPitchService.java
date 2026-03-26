package com.sayai.record.kbo.service;

import com.sayai.record.dto.PitcherDto;
import com.sayai.record.kbo.repository.KboPitchRepository;
import com.sayai.record.kbo.repository.KboPitchStatInterface;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;

@Service
@AllArgsConstructor
@Transactional(readOnly = true)
public class KboPitchService {

    private final KboPitchRepository kboPitchRepository;

    public List<PitcherDto> select(LocalDate startDate, LocalDate endDate) {
        List<KboPitchStatInterface> stats = kboPitchRepository.getStatsByPeriod(startDate, endDate);
        List<PitcherDto> result = new ArrayList<>();
        for (KboPitchStatInterface stat : stats) {
            result.add(mapToDto(stat));
        }
        return result;
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
                .wins(stat.getWins())
                .loses(stat.getLoses())
                .saves(stat.getSaves())
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
