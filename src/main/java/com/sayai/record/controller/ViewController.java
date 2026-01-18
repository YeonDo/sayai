package com.sayai.record.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class ViewController {

    @GetMapping("/")
    public String index() {
        return "index";
    }

    @GetMapping("/fantasy")
    public String fantasy() {
        return "fantasy";
    }

    @GetMapping("/record")
    public String record() {
        return "redirect:/record/game";
    }

    @GetMapping("/record/game")
    public String recordGame() {
        return "record/game";
    }

    @GetMapping("/record/player")
    public String recordPlayer() {
        return "record/player";
    }
}
