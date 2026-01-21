package com.sayai.record.fantasy.service.scoring;

import com.sayai.record.fantasy.entity.FantasyGame;
import com.sayai.record.fantasy.entity.FantasyPlayer;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class RotisserieScoringStrategy implements ScoringStrategy {

    @Override
    public FantasyGame.ScoringType getSupportedType() {
        return FantasyGame.ScoringType.ROTISSERIE;
    }

    @Override
    public double calculateScore(FantasyPlayer player, Map<String, Object> stats, String settingsJson) {
        // Roto logic is complex (ranking across league).
        // This usually happens at League level, not individual player level in isolation.
        // This interface might need refactoring for Roto, but keeping it simple for now.
        return 50.0;
    }
}
