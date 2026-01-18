package com.sayai.record.fantasy.service;

import com.sayai.record.fantasy.entity.FantasyGame;
import com.sayai.record.fantasy.entity.FantasyParticipant;
import com.sayai.record.fantasy.entity.FantasyPlayer;
import com.sayai.record.fantasy.service.rules.DraftRuleValidator;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
public class DraftValidator {

    private final Map<FantasyGame.RuleType, DraftRuleValidator> validatorMap;

    public DraftValidator(List<DraftRuleValidator> validators) {
        this.validatorMap = validators.stream()
                .collect(Collectors.toMap(DraftRuleValidator::getSupportedRuleType, Function.identity()));
    }

    public void validate(FantasyGame game, FantasyPlayer newPlayer, List<FantasyPlayer> currentTeam, FantasyParticipant participant) {
        DraftRuleValidator validator = validatorMap.get(game.getRuleType());
        if (validator == null) {
            throw new IllegalArgumentException("No validator found for rule type: " + game.getRuleType());
        }
        validator.validate(game, newPlayer, currentTeam, participant);
    }
}
