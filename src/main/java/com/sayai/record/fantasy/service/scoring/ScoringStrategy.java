package com.sayai.record.fantasy.service.scoring;

import com.sayai.record.fantasy.entity.FantasyGame;
import com.sayai.record.fantasy.entity.FantasyPlayer;

import java.util.Map;

public interface ScoringStrategy {
    double calculateScore(FantasyPlayer player, Map<String, Object> stats, String settingsJson);
    FantasyGame.ScoringType getSupportedType();
}
