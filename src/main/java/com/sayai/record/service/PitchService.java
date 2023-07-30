package com.sayai.record.service;

import com.sayai.record.dto.PitcherDto;
import com.sayai.record.model.Pitch;
import com.sayai.record.repository.PitchRepository;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


import java.time.LocalDate;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

@Service
@AllArgsConstructor
@Transactional(readOnly = true)
public class PitchService {
    private final PitchRepository pitchRepository;

    @Transactional
    public Pitch save(Pitch pitch){
        return pitchRepository.save(pitch);
    }
    @Transactional
    public void saveAll(List<Pitch> pitchList){
        pitchRepository.saveAll(pitchList);
    }
    public Optional<Pitch> findPitch(Long id){
        return pitchRepository.findById(id);
    }

    public List<PitcherDto> select(LocalDate startDate, LocalDate endDate){
         List<PitcherDto> pitcherTypes =pitchRepository.getStatsByPeriod(startDate,endDate);
         for(PitcherDto p : pitcherTypes){
             Double inn = p.getInn()/3 + (p.getInn()%3)*0.1;
             Double era = (double) (p.getSelfLossScore()*2100/p.getInn())/100.0;
             Double whip = (double) ((p.getPHit()+p.getBaseOnBall()+p.getHitByBall())*300/p.getInn())/100.0;
             Double battingAvg = Math.round(p.getPHit()*1000/p.getHitter())/1000.0;
             Double k9 = (double) (p.getStOut()*2100/p.getInn())/100.0;
             p.setInnings(inn);
             p.setInn(null);
             p.setEra(era);
             p.setWhip(whip);
             p.setBattingAvg(battingAvg);
             p.setK9(k9);
         }
         return pitcherTypes;
    }

    public PitcherDto selectOne(LocalDate startDate, LocalDate endDate, Long id){
        try {
            PitcherDto p = pitchRepository.getStats(startDate, endDate, id).get();
            Double inn = p.getInn()/3 + (p.getInn()%3)*0.1;
            Double era = (p.getSelfLossScore() * 2100 / p.getInn()) / 100.0;
            Double whip = ((p.getPHit() + p.getBaseOnBall() + p.getHitByBall()) * 300 / p.getInn()) / 100.0;
            Double battingAvg = Math.round(p.getPHit() * 1000 / p.getHitter()) / 1000.0;
            Double k9 = (p.getStOut() * 2100 / p.getInn()) / 100.0;
            p.setInnings(inn);
            p.setInn(null);
            p.setEra(era);
            p.setWhip(whip);
            p.setBattingAvg(battingAvg);
            p.setK9(k9);
            return p;
        }catch (NoSuchElementException e){
            return new PitcherDto();
        }
    }
}
