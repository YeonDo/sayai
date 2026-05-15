package com.sayai.record.fantasy.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/fantasy")
public class FantasyViewController {

    @GetMapping("/admin")
    public String admin() {
        return "fantasy/admin";
    }
}
