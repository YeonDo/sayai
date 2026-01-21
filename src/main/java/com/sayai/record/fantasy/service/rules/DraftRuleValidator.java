package com.sayai.record.fantasy.service.rules;

import com.sayai.record.fantasy.entity.FantasyGame;
import com.sayai.record.fantasy.entity.FantasyParticipant;
import com.sayai.record.fantasy.entity.FantasyPlayer;

import java.util.List;

public interface DraftRuleValidator {
    void validate(FantasyGame game, FantasyPlayer newPlayer, List<FantasyPlayer> currentTeam, FantasyParticipant participant);
    FantasyGame.RuleType getSupportedRuleType();
}
