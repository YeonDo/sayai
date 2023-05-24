package com.sayai.record.controller;

import com.sayai.record.dto.PlayerDto;
import com.sayai.record.model.Game;
import com.sayai.record.service.GameService;
import lombok.AllArgsConstructor;
import org.springframework.web.bind.annotation.*;

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
}
