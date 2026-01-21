package com.sayai.record.service;

import com.sayai.record.dto.PitcherDto;
import com.sayai.record.dto.PlayerRecord;
import com.sayai.record.model.Player;
import com.sayai.record.repository.PlayerRepository;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@AllArgsConstructor
@Transactional(readOnly = true)
public class PlayerService {

    private final PlayerRepository playerRepository;
    public List<PlayerRecord> getPlayerList(){
        return playerRepository.findAll().stream()
                .filter(p -> "N".equals(p.getSleepYn()))
                .map(this::toRecord).collect(Collectors.toList());
    }

    public PlayerRecord getPlayer(Long id){
        return toRecord(playerRepository.findById(id).orElseThrow());
    }

    public Optional<Player> getPlayerByName(String name){
        if("임환용".equals(name))
            name = "임강록";
        return playerRepository.findPlayerByName(name);
    }


    public List<PitcherDto> getPitcherList(LocalDate startDate, LocalDate endDate){
        List<PitcherDto> result = new ArrayList<>();
        return result;
    }

    public PlayerRecord toRecord(Player player){
        return new PlayerRecord(player.getId(), player.getBackNo(),player.getName());
    }
}
