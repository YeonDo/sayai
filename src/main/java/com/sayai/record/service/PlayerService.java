package com.sayai.record.service;

import com.sayai.record.dto.PitcherDto;
import com.sayai.record.model.Player;
import com.sayai.record.repository.PlayerRepository;
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
public class PlayerService {

    private final PlayerRepository playerRepository;
    public List<Player> getPlayerList(){
        return playerRepository.findAll();
    }

    public Optional<Player> getPlayer(Long id){
        return playerRepository.findById(id);
    }

    public Optional<Player> getPlayerByName(String name){
        return playerRepository.findPlayerByName(name);
    }


    public List<PitcherDto> getPitcherList(LocalDate startDate, LocalDate endDate){
        List<PitcherDto> result = new ArrayList<>();
        return result;
    }


}
