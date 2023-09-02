package com.sayai.record.service;

import com.sayai.record.dto.LigueRecord;
import com.sayai.record.model.Ligue;
import com.sayai.record.repository.LigueRepository;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@AllArgsConstructor
public class LigueService {
    private final LigueRepository ligueRepository;

    public Optional<Ligue> findByName(String name, Long season){
        return ligueRepository.findBySeasonAndNameOrNameSec(season,name);
    }

    public void saveLeague(LigueRecord ligue){
        ligueRepository.save(toEntity(ligue));
    }

    private Ligue toEntity(LigueRecord dto){
        return Ligue.builder()
                .id(dto.id()).ligIdx(dto.ligIdx())
                .clubId(dto.clubId()).season(dto.season())
                .name(dto.name()).nameSec(dto.nameSec())
                .leagueInfo(dto.leagueInfo()).build();
    }
}
