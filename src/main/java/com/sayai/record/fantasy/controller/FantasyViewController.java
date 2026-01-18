package com.sayai.record.fantasy.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/fantasy")
public class FantasyViewController {

    @GetMapping
    public String dashboard() {
        return "fantasy/index";
    }

    @GetMapping("/my-team")
    public String myTeam() {
        return "fantasy/my-team";
    }

    @GetMapping("/draft/{gameSeq}")
    public String draftRoom(@PathVariable Long gameSeq, Model model) {
        model.addAttribute("gameSeq", gameSeq);
        // We will need to pass playerId for context.
        // For now, let's assume playerId is 1 or passed via query param or handled by JS/Session in a real app.
        // I'll add a placeholder ID for frontend testing in the template.
        model.addAttribute("currentUserId", 1L);
        return "fantasy/draft";
    }

    @GetMapping("/players")
    public String playerList() {
        return "fantasy/players";
    }

    @GetMapping("/trade")
    public String trade() {
        return "fantasy/trade";
    }

    @GetMapping("/settings")
    public String settings() {
        return "fantasy/settings";
    }
}
