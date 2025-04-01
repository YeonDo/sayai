package com.sayai.record.service;

import com.sayai.record.dto.HitterStatDto;
import com.sayai.record.dto.PlayerDto;
import com.sayai.record.dto.PlayerInterface;
import com.sayai.record.model.Hit;
import com.sayai.record.repository.HitRepository;
import com.sayai.record.repository.HitterBoardRepository;
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
    private final HitterBoardRepository hitterBoardRepository;
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

    public List<Hit> findAllHit(){
        return hitRepository.findAll();
    }

    public PlayerDto findOne(LocalDate startDate, LocalDate endDate, Long id){
        try {
            PlayerInterface dto = hitRepository.getPlayerByPeriodAndId(startDate, endDate, id).orElseThrow(NoSuchFieldError::new);
            HitterStatDto statDto = hitterBoardRepository.getPlayerByPeriodAndId(startDate, endDate, id).orElseThrow(NoSuchFieldError::new);
            return interfaceToDto(dto, statDto);
        }catch (NoSuchFieldError e){
            return PlayerDto.builder().build();
        }
    }
    public List<PlayerDto> findAllByPeriod(LocalDate startDate, LocalDate endDate){
        List<PlayerDto> result = new ArrayList<>();
        List<PlayerInterface> dtos = hitRepository.getPlayerByPeriod(startDate, endDate);
        List<HitterStatDto> statDtos = hitterBoardRepository.getPlayerByPeriod(startDate,endDate);
        for(PlayerInterface dto : dtos){
            for(HitterStatDto statDto: statDtos)
                if(statDto.getPlayerId().equals(dto.getId()))
                    result.add(interfaceToDto(dto, statDto));
        }
        return result;
    }
    private PlayerDto interfaceToDto(PlayerInterface dto, HitterStatDto statDto){
        return PlayerDto.builder()
                .id(dto.getId()).name(dto.getName()).backNo(dto.getBackNo())
                .avgPa(dto.getAvgpa()).battingAvg(dto.getBattingavg())
                .atBat(dto.getAtbat()).doubles(dto.getDoubles())
                .homeruns(dto.getHomeruns()).onBasePer(dto.getOnbaseper())
                .slugPer(dto.getSlugper()).totalHits(dto.getTotalhits())
                .singles(dto.getSingles())
                .triples(dto.getTriples()).playerAppearance(dto.getPlayerappearance())
                .baseOnBall(dto.getBaseOnBall())
                .hitByPitch(dto.getHitByPitch())
                .strikeOut(dto.getStrikeOut())
                .ibb(dto.getIbb())
                .dp(dto.getDp())
                .sacrifice(dto.getSacrifice())
                .sacFly(dto.getSacFly())
                .totalGames(dto.getTotalgames())
                .rbi(statDto.getRbi())
                .runs(statDto.getRuns())
                .sb(statDto.getStolenBases())
                .build();
    }
}
