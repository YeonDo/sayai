package com.sayai.kbo.controller;

import com.sayai.kbo.dto.HitterDetailResponse;
import com.sayai.kbo.dto.PitcherDetailResponse;
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
import org.springframework.data.domain.Sort;

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

    private final KboPitchService kboPitchService;
    private final KboHitService kboHitService;

    @GetMapping("/hitter/all")
    @ResponseBody
    public Page<PlayerDto> getAllHitter(
            @RequestParam(required = false) Integer season,
            @RequestParam(value = "start", required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate startDate,
            @RequestParam(value = "end", required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate endDate,
            @RequestParam(value = "sort", required = false) String sort,
            @RequestParam(value = "limit", required = false) Integer limit,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "20") int size) {

        if (season != null) {
            PageRequest pageable = buildPageable(page, size, sort);
            return kboHitService.findAllBySeason(season, limit, pageable);
        } else if (startDate != null && endDate != null) {
            return kboHitService.findAllByPeriod(startDate, endDate, PageRequest.of(page, size));
        } else {
            throw new IllegalArgumentException("season 또는 start+end 파라미터가 필요합니다.");
        }
    }

    @GetMapping("/pitcher/all")
    @ResponseBody
    public Page<PitcherDto> getAllPitcher(
            @RequestParam(required = false) Integer season,
            @RequestParam(value = "start", required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate startDate,
            @RequestParam(value = "end", required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate endDate,
            @RequestParam(value = "sort", required = false) String sort,
            @RequestParam(value = "limit", required = false) Integer limit,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "20") int size) {

        if (season != null) {
            // limit은 이닝(outs/3) 기준 → minOuts = limit * 3
            Integer minOuts = limit != null ? limit * 3 : null;
            PageRequest pageable = buildPageable(page, size, sort);
            return kboPitchService.selectBySeason(season, minOuts, pageable);
        } else if (startDate != null && endDate != null) {
            return kboPitchService.select(startDate, endDate, PageRequest.of(page, size));
        } else {
            throw new IllegalArgumentException("season 또는 start+end 파라미터가 필요합니다.");
        }
    }

    /**
     * sort 파라미터를 파싱하여 PageRequest를 생성합니다.
     * 형식: {field}_{direction} (예: hr_desc, pa_asc)
     */
    private PageRequest buildPageable(int page, int size, String sort) {
        if (sort == null || sort.isBlank()) {
            return PageRequest.of(page, size);
        }
        int lastUnderscore = sort.lastIndexOf('_');
        if (lastUnderscore < 1) {
            return PageRequest.of(page, size);
        }
        String field = sort.substring(0, lastUnderscore);
        String direction = sort.substring(lastUnderscore + 1);
        Sort.Direction sortDirection = "asc".equalsIgnoreCase(direction) ? Sort.Direction.ASC : Sort.Direction.DESC;
        return PageRequest.of(page, size, Sort.by(sortDirection, field));
    }

    @GetMapping("/hitter/{playerId}")
    @ResponseBody
    public HitterDetailResponse getHitter(@PathVariable("playerId") Long playerId,
                                          @RequestParam("start") @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate startDate,
                                          @RequestParam("end") @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate endDate,
                                          @RequestParam(value = "page", defaultValue = "0") int page,
                                          @RequestParam(value = "size", defaultValue = "10") int size) {
        return kboHitService.findOneWithDailyStats(startDate, endDate, playerId, PageRequest.of(page, size));
    }

    @GetMapping("/pitcher/{playerId}")
    @ResponseBody
    public PitcherDetailResponse getPitcher(@PathVariable("playerId") Long playerId,
                                            @RequestParam("start") @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate startDate,
                                            @RequestParam("end") @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate endDate,
                                            @RequestParam(value = "page", defaultValue = "0") int page,
                                            @RequestParam(value = "size", defaultValue = "10") int size) {
        return kboPitchService.selectOneWithDailyStats(startDate, endDate, playerId, PageRequest.of(page, size));
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
