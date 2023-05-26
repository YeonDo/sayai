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
    public ResponseDto crawling(@RequestParam(value = "url") String url){
        String urlForm ="http://www.gameone.kr/club/info/schedule/boxscore?club_idx=15387&game_idx=";
        crawlingService.crawl(urlForm+url);
        return ResponseDto.builder().resultMsg("Success").build();
    }

    @PutMapping("/opponent")
    @ResponseBody
    public ResponseDto updateOpponent() throws IOException {
        crawlingService.updateOp();
        return ResponseDto.builder().resultMsg("Every Process Success").build();
    }
}
