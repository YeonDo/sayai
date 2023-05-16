package com.sayai.record.service;

import com.sayai.record.model.Game;
import com.sayai.record.repository.GameRepository;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import javax.transaction.Transactional;
import java.util.Optional;

@Service
@AllArgsConstructor
public class GameService {
    private final GameRepository gameRepository;

    @Transactional
    public Game saveGame(Game game){
        return gameRepository.save(game);
    }

    public Optional<Game> findGame(Long id){
        return gameRepository.findById(id);
    }


}
