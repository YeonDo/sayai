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
        // Global constraint: Max 1 CL player across any rule type
        long clCount = currentTeam.stream()
                .filter(p -> {
                    String pos = p.getPosition();
                    if (pos == null || pos.isEmpty()) return false;
                    return java.util.Arrays.asList(pos.split(",")).stream()
                            .map(String::trim)
                            .anyMatch(s -> s.equals("CL"));
                })
                .count();

        // Check if the new player also has CL
        boolean isNewPlayerCl = false;
        if (newPlayer.getPosition() != null && !newPlayer.getPosition().isEmpty()) {
            isNewPlayerCl = java.util.Arrays.asList(newPlayer.getPosition().split(",")).stream()
                    .map(String::trim)
                    .anyMatch(s -> s.equals("CL"));
        }

        if (clCount >= 1 && isNewPlayerCl) {
            throw new IllegalStateException("CL 포지션은 1명까지만 선발할 수 있습니다.");
        }

        DraftRuleValidator validator = validatorMap.get(game.getRuleType());
        if (validator == null) {
            throw new IllegalArgumentException("No validator found for rule type: " + game.getRuleType());
        }
        validator.validate(game, newPlayer, currentTeam, participant);
    }

    public int getTotalPlayerCount(FantasyGame.RuleType ruleType) {
        DraftRuleValidator validator = validatorMap.get(ruleType);
        if (validator == null) {
            throw new IllegalArgumentException("No validator found for rule type: " + ruleType);
        }
        return validator.getTotalPlayerCount();
    }
}
