package com.sayai.record.controller;

import com.sayai.record.dto.PitcherDto;
import com.sayai.record.dto.PlayerDto;
import com.sayai.record.dto.PlayerRecord;
import com.sayai.record.dto.ResponseDto;
import com.sayai.record.model.Player;
import com.sayai.record.service.GameService;
import com.sayai.record.service.HitService;
import com.sayai.record.service.PitchService;
import com.sayai.record.service.PlayerService;
import com.sayai.record.util.Utils;
import lombok.AllArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@AllArgsConstructor
@RestController
@CrossOrigin(origins = "*", allowedHeaders = "*")
@RequestMapping("/apis/v1/player")
public class PlayerController {
    private final PlayerService playerService;
    private final PitchService pitchService;
    private final HitService hitService;
    @GetMapping("/{id}")
    @ResponseBody
    public PlayerRecord getPlayer(@PathVariable("id") Long id){
        return playerService.getPlayer(id);
    }
    @GetMapping("/all")
    @ResponseBody
    public List<PlayerRecord> getAllPlayers(){
        return playerService.getPlayerList();
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
    @GetMapping("/hitter/{playerId}")
    @ResponseBody
    public PlayerDto getHitter(@PathVariable("playerId") Long playerId, @RequestParam("start") @DateTimeFormat(pattern="yyyy-MM-dd") LocalDate startDate, @RequestParam("end") @DateTimeFormat(pattern="yyyy-MM-dd") LocalDate endDate){
        return hitService.findOne(startDate,endDate,playerId);
    }

    @GetMapping("/pitcher/{playerId}")
    @ResponseBody
    public PitcherDto getPitcher(@PathVariable("playerId") Long playerId,@RequestParam("start") @DateTimeFormat(pattern="yyyy-MM-dd") LocalDate startDate, @RequestParam("end") @DateTimeFormat(pattern="yyyy-MM-dd") LocalDate endDate){
        return pitchService.selectOne(startDate,endDate,playerId);
    }

    @GetMapping("/hitter/{playerId}/period")
    @ResponseBody
    public List<PlayerDto> getHitPeriod(@PathVariable("playerId") Long playerId,@RequestParam("list") String[] periodList){
        List result = new ArrayList();
        Utils utils = new Utils();
        for(String s : periodList){
            if(s.equals("total")) {
                PlayerDto dto = hitService.findOne(LocalDate.of(2012, 1, 1), LocalDate.now(), playerId);
                dto.setSeason("total");
                result.add(dto);
            }
            else if(s.length()==4){
                int year = Integer.parseInt(s);
                PlayerDto dto = hitService.findOne(LocalDate.of(year, 1, 1), LocalDate.of(year, 12, 31), playerId);
                dto.setSeason(s);
                result.add(dto);
            }else{
                if(s.length()!=6)
                    continue;
                int year = Integer.parseInt(s.substring(0,4));
                int month = Integer.parseInt(s.substring(4,6));
                int lastDayOfMonth = utils.getLastDayOfMonth(year, month);
                PlayerDto dto = hitService.findOne(LocalDate.of(year, month, 1), LocalDate.of(year, month, lastDayOfMonth), playerId);
                dto.setSeason(s);
                result.add(dto);
            }
        }
        return result;
    }


}
