package com.sayai.record.fantasy.controller;

import com.sayai.record.fantasy.entity.RoasterLog;
import com.sayai.record.fantasy.repository.RoasterLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/apis/v1/fantasy")
@RequiredArgsConstructor
public class FantasyLogController {

    private final RoasterLogRepository roasterLogRepository;

    @GetMapping("/games/{gameSeq}/logs")
    public ResponseEntity<List<RoasterLog>> getLogs(@PathVariable(name = "gameSeq") Long gameSeq) {
        return ResponseEntity.ok(roasterLogRepository.findByFantasyGameSeqOrderByTimestampDesc(gameSeq));
    }
}
