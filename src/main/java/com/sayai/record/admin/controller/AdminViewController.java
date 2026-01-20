package com.sayai.record.admin.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/admin")
public class AdminViewController {

    @GetMapping
    public String index() {
        return "admin/index";
    }

    @GetMapping("/games/create")
    public String createGame() {
        return "admin/game-create";
    }

    @GetMapping("/users")
    public String users() {
        return "admin/users";
    }
}
