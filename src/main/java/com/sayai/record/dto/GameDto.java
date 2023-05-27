package com.sayai.record.dto;

import lombok.*;

import java.time.LocalDate;
import java.time.LocalTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
public class GameDto {
    private Long id;
    private Long season;
    private String fl;
    private String stadium;
    private LocalDate gameDate;
    private LocalTime gameTime;
    private String opponent;
    private String result;
    private Long homeScore;
    private Long awayScore;
    private String scorebox;
    private String gameoneLink;
}
