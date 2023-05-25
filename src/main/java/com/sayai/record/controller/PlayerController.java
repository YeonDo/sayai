package com.sayai.record.controller;

import com.sayai.record.dto.PitcherDto;
import com.sayai.record.dto.PlayerDto;
import com.sayai.record.dto.ResponseDto;
import com.sayai.record.model.Player;
import com.sayai.record.service.GameService;
import com.sayai.record.service.HitService;
import com.sayai.record.service.PitchService;
import com.sayai.record.service.PlayerService;
import lombok.AllArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@AllArgsConstructor
@RestController
@RequestMapping("/apis/v1/player")
public class PlayerController {
    private final PlayerService playerService;
    private final PitchService pitchService;
    private final HitService hitService;
    @GetMapping("/{id}")
    @ResponseBody
    public PlayerDto getPlayer(@PathVariable Long id){
        System.out.println("========================");
        return playerService.getPlayer(id).get().toDto();
    }
    @GetMapping("/all")
    @ResponseBody
    public List<PlayerDto> getAllPlayers(){
        List<Player> playerList = playerService.getPlayerList();
        List<PlayerDto> result = new ArrayList<>();
        for(Player p : playerList){
            result.add(p.toDto());
        }
        return result;
    }
    @GetMapping("/hitter/all")
    @ResponseBody
    public List<PlayerDto> getAllHitter(@RequestParam("start") @DateTimeFormat(pattern="yyyy-MM-dd") LocalDate startDate, @RequestParam("end") @DateTimeFormat(pattern="yyyy-MM-dd") LocalDate endDate){
        return hitService.findAllByPeriod(startDate,endDate);
    }
    @GetMapping("/pitcher/all")
    @ResponseBody
    public List<PitcherDto> getAllPitcher(@RequestParam("start") @DateTimeFormat(pattern="yyyy-MM-dd") LocalDate startDate, @RequestParam("end") @DateTimeFormat(pattern="yyyy-MM-dd") LocalDate endDate){
        List<PitcherDto> result = pitchService.select(startDate,endDate);
        return result;
    }
    @GetMapping("/hitter/{playerid}")
    @ResponseBody
    public PlayerDto getHitter(@PathVariable Long playerid,@RequestParam("start") @DateTimeFormat(pattern="yyyy-MM-dd") LocalDate startDate, @RequestParam("end") @DateTimeFormat(pattern="yyyy-MM-dd") LocalDate endDate){
        return hitService.findOne(startDate,endDate,playerid);
    }

    @GetMapping("/pitcher/{playerid}")
    @ResponseBody
    public PitcherDto getPitcher(@PathVariable Long playerid,@RequestParam("start") @DateTimeFormat(pattern="yyyy-MM-dd") LocalDate startDate, @RequestParam("end") @DateTimeFormat(pattern="yyyy-MM-dd") LocalDate endDate){
        return pitchService.selectOne(startDate,endDate,playerid);
    }
}
