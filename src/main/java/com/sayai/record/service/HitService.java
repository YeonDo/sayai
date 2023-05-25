package com.sayai.record.service;

import com.sayai.record.dto.PlayerDto;
import com.sayai.record.dto.PlayerInterface;
import com.sayai.record.model.Hit;
import com.sayai.record.repository.HitRepository;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@AllArgsConstructor
@Transactional(readOnly = true)
public class HitService {
    private final HitRepository hitRepository;
    @Transactional
    public Hit saveHit(Hit hit){
        return hitRepository.save(hit);
    }
    @Transactional
    public void saveAll(List<Hit> hitList){
        hitRepository.saveAll(hitList);
    }
    public Optional<Hit> findHit(Long id){
        return hitRepository.findById(id);
    }

    public List<Hit> findAllHit(Long playerid){
        return hitRepository.findAll();
    }

    public PlayerDto findOne(LocalDate startDate, LocalDate endDate, Long id){
        PlayerInterface dto = hitRepository.getPlayerByPeriodAndId(startDate, endDate, id).get();
        return PlayerDto.builder()
                .id(dto.getId()).name(dto.getName()).backNo(dto.getBackNo())
                .avgPa(dto.getAvgpa()).battingAvg(dto.getBattingavg())
                .atBat(dto.getAtbat()).doubles(dto.getDoubles())
                .homeruns(dto.getHomeruns()).onBasePer(dto.getOnbaseper())
                .slugPer(dto.getSlugper()).totalHits(dto.getTotalhits())
                .singles(dto.getSingles())
                .triples(dto.getTriples()).playerAppearance(dto.getPlayerappearance())
                .totalGames(dto.getTotalgames()).build();
    }
    public List<PlayerDto> findAllByPeriod(LocalDate startDate, LocalDate endDate){
        List<PlayerDto> result = new ArrayList<>();
        List<PlayerInterface> dtos = hitRepository.getPlayerByPeriod(startDate, endDate);
        for(PlayerInterface dto : dtos){
            result.add(PlayerDto.builder()
                    .id(dto.getId()).name(dto.getName()).backNo(dto.getBackNo())
                    .avgPa(dto.getAvgpa()).battingAvg(dto.getBattingavg())
                    .atBat(dto.getAtbat()).doubles(dto.getDoubles())
                    .homeruns(dto.getHomeruns()).onBasePer(dto.getOnbaseper())
                    .slugPer(dto.getSlugper()).totalHits(dto.getTotalhits())
                    .singles(dto.getSingles())
                    .triples(dto.getTriples()).playerAppearance(dto.getPlayerappearance())
                    .totalGames(dto.getTotalgames()).build());
        }
        return result;
    }
}
