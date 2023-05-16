package com.sayai.record.service;

import com.sayai.record.model.Player;
import com.sayai.record.repository.PlayerRepository;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@AllArgsConstructor
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
}
