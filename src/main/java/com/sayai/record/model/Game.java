package com.sayai.record.model;


import com.sayai.record.model.enums.FirstLast;
import lombok.*;

import javax.persistence.*;
import java.sql.Time;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "GAME")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class Game {

    @Id
    @Column(name = "GAME_IDX")
    private Long id;

    private Long season;

    private Long leagueId;

    private Long clubId;

    @Enumerated(EnumType.STRING)
    @Column(name = "FIRST_LAST")
    private FirstLast fl;

    private String stadium;

    private LocalDate gameDate;

    private LocalTime gameTime;
    private String opponent;
    private Long homeScore;
    private Long awayScore;
    private String result;
    @Builder.Default
    @OneToMany(mappedBy = "game", cascade = CascadeType.ALL)
    private List<Pitch> pitchList = new ArrayList<>();
    @Builder.Default
    @OneToMany(mappedBy = "game", cascade = CascadeType.ALL)
    private List<Hit> hitList = new ArrayList<>();

    public void setOpponent(String opponent) {
        this.opponent = opponent;
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

    public void updateLeague(Long leagueId){
        this.leagueId = leagueId;
    }
}
