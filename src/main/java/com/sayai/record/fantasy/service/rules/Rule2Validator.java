package com.sayai.record.fantasy.service.rules;

import com.sayai.record.fantasy.entity.FantasyGame;
import com.sayai.record.fantasy.entity.FantasyParticipant;
import com.sayai.record.fantasy.entity.FantasyPlayer;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class Rule2Validator extends Rule1Validator {

    @Override
    public FantasyGame.RuleType getSupportedRuleType() {
        return FantasyGame.RuleType.RULE_2;
    }

    @Override
    public void validate(FantasyGame game, FantasyPlayer newPlayer, List<FantasyPlayer> currentTeam, FantasyParticipant participant) {
        // Rule 2 Specific Check
        if (currentTeam.isEmpty()) {
            if (participant == null || participant.getPreferredTeam() == null) {
                throw new IllegalStateException("Preferred team not set for participant");
            }

            // Normalize team names for comparison if needed
            // Use contains to support Short Name vs Full Name (e.g., KIA vs KIA Tigers)
            String pref = participant.getPreferredTeam().trim();
            String playerTeam = newPlayer.getTeam().trim();

            boolean match = playerTeam.toLowerCase().contains(pref.toLowerCase()) ||
                            pref.toLowerCase().contains(playerTeam.toLowerCase());

            if (!match) {
                throw new IllegalStateException("First pick must be from preferred team: " + participant.getPreferredTeam());
            }
        }

        // Delegate to Rule 1 (Base Composition Check)
        super.validate(game, newPlayer, currentTeam, participant);
    }
}
