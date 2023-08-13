package com.sayai.record.repository;

import com.sayai.record.model.Game;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
@SpringBootTest
class GameRepositoryTest {
    @Autowired
    GameRepository gameRepository;
    @Test
    void findAllByOpponentMatchAgainst() {
        List<Game> opponentMatchAgainst = gameRepository.findAllByOpponentMatchAgainst("레드소울");
        for(Game g: opponentMatchAgainst){
            System.out.println(g.getOpponent());
            System.out.println(g.getId());
        }

    }
}