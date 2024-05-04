package com.sayai.record.viewer.service;



import com.sayai.record.viewer.dto.Episode;
import com.sayai.record.viewer.dto.EpisodeDto;
import org.springframework.stereotype.Service;

import java.util.LinkedList;

@Service
public class EpisodeService {
    private final static LinkedList<EpisodeDto> set = new LinkedList<>();

    public EpisodeService(){
        Episode[] episodes = Episode.values();
        int i = 1;
        for(Episode episode: episodes) {
            set.add(new EpisodeDto(i,episode.getNumber(),episode.getTitle(),episode.getLink(),episode.getTags()));
            i++;
        }
    }
    public EpisodeDto getEpisode(int episodeNum){
        if(episodeNum>=set.size())
            return null;
        return set.get(episodeNum);
    }
}
