package com.sayai.record.viewer.controller;

import com.sayai.record.viewer.service.EpisodeService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import com.sayai.record.viewer.dto.Episode;
import com.sayai.record.viewer.dto.EpisodeDto;

@Controller
@RequiredArgsConstructor
public class EpisodeController {
    private final EpisodeService episodeService;
    @GetMapping("/episodes/{episodeNumber}")
    public String getEpisode(@PathVariable("episodeNumber") int episodeNumber, Model model) {
        EpisodeDto episode = episodeService.getEpisode(episodeNumber);
        if(episode == null)
            return "error";
        if(episode.getNumber() >0) {
            int num = episode.getNumber();
            model.addAttribute("episodeTitle", num +". "+ episode.getTitle());
        }else{
            model.addAttribute("episodeTitle", episode.getTitle());
        }
        model.addAttribute("comicLink", episode.getLink());


        // 해당 에피소드에 해당하는 HTML을 렌더링할 Thymeleaf 템플릿의 이름을 반환합니다.
        return "episode";
    }

}
