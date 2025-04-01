package com.sayai.record.controller;

import com.sayai.record.dto.ResponseDto;
import com.sayai.record.service.CrawlingService;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;

@RestController
@AllArgsConstructor
@RequestMapping("/apis/v1/crawl")
public class CrawlingController {
    private final CrawlingService crawlingService;

    @GetMapping
    @ResponseBody
    public ResponseDto crawling(@RequestParam(value = "url") String url, @RequestParam(value="season", required = false) Long season){
        String urlForm ="http://www.gameone.kr/club/info/schedule/boxscore?club_idx=15387&game_idx=";
        return crawlingService.crawl(urlForm+url,season);

    }

    @PutMapping("/opponent")
    @ResponseBody
    public ResponseDto updateOpponent() throws IOException {
        crawlingService.updateOp();
        return ResponseDto.builder().resultMsg("Every Process Success").build();
    }

    @PutMapping("/history")
    @ResponseBody
    public String updateSince(@RequestParam("year") Integer year, @RequestParam("page") Integer page){
        crawlingService.updateSince(year, page);
        return "OK";
    }
    @PutMapping("/history/pages")
    @ResponseBody
    public String updateSince(@RequestParam("year") Integer year){
        crawlingService.updateSince(year);
        return "OK";
    }
    @PutMapping("/league")
    @ResponseBody
    public ResponseDto updateLeagueInfo(){
        ResponseDto responseDto = crawlingService.updateAllLeagueInfo();
        if(responseDto.getResultCode()>0)
            throw new RuntimeException();
        return responseDto;
    }
}
