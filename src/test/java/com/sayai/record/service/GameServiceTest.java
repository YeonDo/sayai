package com.sayai.record.service;

import com.sayai.record.dto.GameDto;
import net.bytebuddy.asm.Advice;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import jakarta.transaction.Transactional;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
@SpringBootTest
@Transactional
class GameServiceTest {
    @Autowired
    GameService gameService;
    @Test
    void findMatches() {
        List<GameDto> matches = gameService.findMatches(LocalDate.of(2023, 01, 01), LocalDate.of(2023, 12, 31));
    }

    //@Test
    void findOpponent() {
        List<GameDto> games = gameService.findOpponent("레드쏘울");
        System.out.println(games);
    }
}