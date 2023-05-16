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
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class Game {

    @Id
    @Column(name = "GAME_IDX")
    private Long id;

    private Long season;

    private Long ligIdx;

    private Long clubId;

    @Enumerated(EnumType.STRING)
    @Column(name = "FIRST_LAST")
    private FirstLast fl;

    private String stadium;

    private LocalDate gameDate;

    private LocalTime gameTime;
    private String opponent;
    @Builder.Default
    @OneToMany(mappedBy = "game", fetch = FetchType.LAZY)
    private List<Pitch> pitchList = new ArrayList<>();
    @Builder.Default
    @OneToMany(mappedBy = "game", fetch = FetchType.LAZY)
    private List<Hit> hitList = new ArrayList<>();
}
