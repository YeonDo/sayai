package com.sayai.record.controller;

import com.sayai.record.dto.GameDto;
import com.sayai.record.dto.PlayerDto;
import com.sayai.record.model.Game;
import com.sayai.record.service.GameService;
import lombok.AllArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@AllArgsConstructor
@RestController
@RequestMapping("/apis/v1/game")
public class GameController {
    private final GameService gameService;

    @GetMapping("/recent")
    public String getRecent(){
        Game recent = gameService.findRecent();
        return "Last Match Recorded is : "+ recent.getGameDate();
    }

    @GetMapping("/list")
    public List<GameDto> getGameList(@RequestParam("start") @DateTimeFormat(pattern="yyyy-MM-dd") LocalDate startDate, @RequestParam("end") @DateTimeFormat(pattern="yyyy-MM-dd") LocalDate endDate){
        return gameService.findMatches(startDate,endDate);
    }

    @GetMapping("/opponent/{opponent}")
    public List<GameDto> getOpponent(@PathVariable("opponent") String opponent){
        return gameService.findOpponent(opponent);
    }
}
