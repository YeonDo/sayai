package com.sayai.record.fantasy.controller;

import com.sayai.record.auth.entity.Member;
import com.sayai.record.auth.repository.MemberRepository;
import com.sayai.record.fantasy.service.FantasyGameService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/fantasy")
@RequiredArgsConstructor
public class FantasyViewController {

    private final FantasyGameService fantasyGameService;
    private final MemberRepository memberRepository;

    @ModelAttribute
    public void addAttributes(Model model, @AuthenticationPrincipal UserDetails userDetails) {
        if (userDetails != null) {
            Long playerId = memberRepository.findByUserId(userDetails.getUsername())
                    .map(Member::getPlayerId)
                    .orElse(null);

            if (playerId != null) {
                model.addAttribute("currentUserId", playerId);
                Long draftingGameSeq = fantasyGameService.findDraftingGameId(playerId);
                model.addAttribute("draftingGameSeq", draftingGameSeq);
            }
        }
    }

    @GetMapping
    public String dashboard() {
        return "fantasy/index";
    }

    @GetMapping("/my-team")
    public String myTeam() {
        return "fantasy/my-team";
    }

    @GetMapping("/draft/{gameSeq}")
    public String draftRoom(@PathVariable("gameSeq") Long gameSeq, Model model) {
        model.addAttribute("gameSeq", gameSeq);
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

    @GetMapping("/admin")
    public String admin() {
        return "fantasy/admin";
    }

    @GetMapping("/draft-log")
    public String draftLog() {
        return "fantasy/draft-log";
    }
}
