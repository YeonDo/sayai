package com.sayai.record.fantasy.service.scoring;

import com.sayai.record.fantasy.entity.FantasyGame;
import com.sayai.record.fantasy.entity.FantasyPlayer;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class PointScoringStrategy implements ScoringStrategy {

    @Override
    public FantasyGame.ScoringType getSupportedType() {
        return FantasyGame.ScoringType.POINTS;
    }

    @Override
    public double calculateScore(FantasyPlayer player, Map<String, Object> stats, String settingsJson) {
        // Mock implementation. Real logic needs JSON parsing of settings and stats mapping.
        // For now, return a dummy score.
        return 100.0;
    }
}
