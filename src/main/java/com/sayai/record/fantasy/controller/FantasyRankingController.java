package com.sayai.record.fantasy.controller;

import com.sayai.record.fantasy.dto.RankingTableDto;
import com.sayai.record.fantasy.service.FantasyRankingService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/apis/v1/fantasy/games")
@RequiredArgsConstructor
public class FantasyRankingController {

    private final FantasyRankingService fantasyRankingService;

    @GetMapping("/{gameSeq}/ranking")
    public RankingTableDto getRanking(@PathVariable("gameSeq") Long gameSeq) {
        return fantasyRankingService.getRanking(gameSeq);
    }
}
