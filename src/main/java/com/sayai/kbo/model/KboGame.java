package com.sayai.kbo.model;

import lombok.*;

import jakarta.persistence.*;
import java.util.ArrayList;
import java.util.List;
import java.time.LocalDate;

@Entity
@Table(name = "kbo_game")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class KboGame {

    @Id
    @Column(name = "game_idx")
    private Long id;

    @Column(name = "season")
    private Long season;


    @Column(name = "home")
    private String home;

    @Column(name = "away")
    private String away;

    @Column(name = "home_score")
    private Long homeScore;

    @Column(name = "away_score")
    private Long awayScore;

    @Column(name = "result")
    private String result;



    public void setHome(String home) {
        this.home = home;
    }

    public void setAway(String away) {
        this.away = away;
    }

    public void setHomeScore(Long homeScore) {
        this.homeScore = homeScore;
    }

    public void setAwayScore(Long awayScore) {
        this.awayScore = awayScore;
    }

    public void setResult(String result) {
        this.result = result;
    }

}
