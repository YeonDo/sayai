package com.sayai.kbo.model;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class KboPitcherStatsId implements Serializable {
    private Long playerId;
    private Integer season;
}
