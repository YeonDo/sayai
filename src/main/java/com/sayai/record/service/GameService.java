package com.sayai.record.service;

import com.sayai.record.model.Game;
import com.sayai.record.repository.GameRepository;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


import java.util.List;
import java.util.Optional;

@Service
@AllArgsConstructor
@Transactional(readOnly = true)
public class GameService {
    private final GameRepository gameRepository;

    @Transactional
    public Game saveGame(Game game){
        return gameRepository.save(game);
    }

    public Optional<Game> findGame(Long id){
        return gameRepository.findById(id);
    }

    public List<Game> findAll(){ return gameRepository.findAll();}

    public Game findRecent(){return gameRepository.findFirstByOrderByGameDateDesc().get();}
}
