package com.sayai.kbo.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/admin")
public class KboAdminViewController {

    @GetMapping("/kbo-stats")
    public String getKboStatsAdminPage() {
        return "admin/KBO-stats";
    }
}
