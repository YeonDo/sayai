package com.sayai.record.controller;

import com.sayai.record.service.PlayerService;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Controller;

@Controller
@AllArgsConstructor
public class PlayerController {
    private final PlayerService playerService;

}
