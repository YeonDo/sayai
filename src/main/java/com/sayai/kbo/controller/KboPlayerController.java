package com.sayai.kbo.controller;

import com.sayai.record.dto.PitcherDto;
import com.sayai.record.dto.PlayerDto;
import com.sayai.record.dto.PlayerRecord;
import com.sayai.kbo.service.KboHitService;
import com.sayai.kbo.service.KboPitchService;
import com.sayai.record.service.PlayerService;
import com.sayai.record.util.Utils;
import lombok.AllArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@AllArgsConstructor
@RestController
@CrossOrigin(origins = "*", allowedHeaders = "*")
@RequestMapping("/apis/v1/kboplayer")
public class KboPlayerController {

    private final PlayerService playerService;
    private final KboPitchService kboPitchService;
    private final KboHitService kboHitService;

    @GetMapping("/{id}")
    @ResponseBody
    public PlayerRecord getPlayer(@PathVariable("id") Long id) {
        return playerService.getPlayer(id);
    }

    @GetMapping("/hitter/all")
    @ResponseBody
    public Page<PlayerDto> getAllHitter(@RequestParam("start") @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate startDate,
                                        @RequestParam("end") @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate endDate,
                                        @RequestParam(value = "page", defaultValue = "0") int page,
                                        @RequestParam(value = "size", defaultValue = "20") int size) {
        return kboHitService.findAllByPeriod(startDate, endDate, PageRequest.of(page, size));
    }

    @GetMapping("/pitcher/all")
    @ResponseBody
    public Page<PitcherDto> getAllPitcher(@RequestParam("start") @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate startDate,
                                          @RequestParam("end") @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate endDate,
                                          @RequestParam(value = "page", defaultValue = "0") int page,
                                          @RequestParam(value = "size", defaultValue = "20") int size) {
        return kboPitchService.select(startDate, endDate, PageRequest.of(page, size));
    }

    @GetMapping("/hitter/{playerId}")
    @ResponseBody
    public PlayerDto getHitter(@PathVariable("playerId") Long playerId,
                               @RequestParam("start") @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate startDate,
                               @RequestParam("end") @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate endDate) {
        return kboHitService.findOne(startDate, endDate, playerId);
    }

    @GetMapping("/pitcher/{playerId}")
    @ResponseBody
    public PitcherDto getPitcher(@PathVariable("playerId") Long playerId,
                                 @RequestParam("start") @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate startDate,
                                 @RequestParam("end") @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate endDate) {
        return kboPitchService.selectOne(startDate, endDate, playerId);
    }

    @GetMapping("/hitter/{playerId}/period")
    @ResponseBody
    public List<PlayerDto> getHitPeriod(@PathVariable("playerId") Long playerId, @RequestParam("list") String[] periodList) {
        List<PlayerDto> result = new ArrayList<>();
        Utils utils = new Utils();
        for (String s : periodList) {
            if (s.equals("total")) {
                PlayerDto dto = kboHitService.findOne(LocalDate.of(2012, 1, 1), LocalDate.now(), playerId);
                dto.setSeason("total");
                result.add(dto);
            } else if (s.length() == 4) {
                int year = Integer.parseInt(s);
                PlayerDto dto = kboHitService.findOne(LocalDate.of(year, 1, 1), LocalDate.of(year, 12, 31), playerId);
                dto.setSeason(s);
                result.add(dto);
            } else {
                if (s.length() != 6) continue;
                int year = Integer.parseInt(s.substring(0, 4));
                int month = Integer.parseInt(s.substring(4, 6));
                int lastDayOfMonth = utils.getLastDayOfMonth(year, month);
                PlayerDto dto = kboHitService.findOne(LocalDate.of(year, month, 1), LocalDate.of(year, month, lastDayOfMonth), playerId);
                dto.setSeason(s);
                result.add(dto);
            }
        }
        return result;
    }
}
