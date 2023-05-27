package com.sayai.record.service;

import com.sayai.record.dto.GameDto;
import com.sayai.record.model.Game;
import com.sayai.record.model.enums.FirstLast;
import com.sayai.record.repository.GameRepository;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@AllArgsConstructor
@Transactional(readOnly = true)
public class GameService {
    private final GameRepository gameRepository;

    @Transactional
    public Game saveGame(Game game){
        return gameRepository.save(game);
    }

    public Optional<Game> findGame(Long id){
        return gameRepository.findById(id);
    }
    public List<Game> findAll(){return gameRepository.findAll();}
    public List<GameDto> findMatches(LocalDate startDate, LocalDate endDate){
        List<Game> gameList = gameRepository.findAllByGameDateBetweenOrderByGameDateAscGameTimeAsc(startDate, endDate);
        List<GameDto> result = new ArrayList<>();
        for(Game g : gameList){
            FirstLast fl = g.getFl();
            String hometm= "팀 사야이";
            String awaytm = "팀 사야이";
            if(fl.equals(FirstLast.F)) awaytm = g.getOpponent();
            else hometm = g.getOpponent();
            String scorebox = hometm + " " + g.getHomeScore() + " : " + g.getAwayScore() + " " + awaytm;
            String link = "http://www.gameone.kr/club/info/schedule/boxscore?club_idx=15387&game_idx="+g.getId();
            result.add(GameDto.builder()
                    .id(g.getId()).season(g.getSeason()).fl(g.getFl().toString())
                    .stadium(g.getStadium()).gameDate(g.getGameDate())
                    .gameTime(g.getGameTime()).opponent(g.getOpponent())
                    .homeScore(g.getHomeScore()).awayScore(g.getAwayScore())
                    .result(g.getResult()).scorebox(scorebox)
                    .gameoneLink(link).build());
        }
        return result;
    }

    public Game findRecent(){return gameRepository.findFirstByOrderByGameDateDesc().get();}

    public List<GameDto> findOpponent(String opponent){
        List<Game> gameList = gameRepository.findByOpponentContainingOrderByGameDateAsc(opponent);
        List<GameDto> result = new ArrayList<>();
        for(Game g : gameList){
            FirstLast fl = g.getFl();
            String hometm= "팀 사야이";
            String awaytm = "팀 사야이";
            if(fl.equals(FirstLast.F)) awaytm = g.getOpponent();
            else hometm = g.getOpponent();
            String scorebox = hometm + " " + g.getHomeScore() + " : " + g.getAwayScore() + " " + awaytm;
            String link = "http://www.gameone.kr/club/info/schedule/boxscore?club_idx=15387&game_idx="+g.getId();
            result.add(GameDto.builder()
                    .id(g.getId()).season(g.getSeason()).fl(g.getFl().toString())
                    .stadium(g.getStadium()).gameDate(g.getGameDate())
                    .gameTime(g.getGameTime()).opponent(g.getOpponent())
                    .homeScore(g.getHomeScore()).awayScore(g.getAwayScore())
                    .result(g.getResult()).scorebox(scorebox)
                    .gameoneLink(link).build());
        }
        return result;
    }
}
