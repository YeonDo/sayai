package com.sayai.record.fantasy.service.rules;

import com.sayai.record.fantasy.entity.FantasyGame;
import com.sayai.record.fantasy.entity.FantasyParticipant;
import com.sayai.record.fantasy.entity.FantasyPlayer;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class Rule2Validator extends Rule1Validator {

    @Override
    public FantasyGame.RuleType getSupportedRuleType() {
        return FantasyGame.RuleType.RULE_2;
    }

    @Override
    public int getTotalPlayerCount() {
        return 20;
    }

    // Reuse Rule1Validator.validate() which performs:
    // 1. First Pick Rule Check (conditional)
    // 2. Composition Check (calls this.canFit())
    // 3. Foreigner Limit Check
    // 4. Team Restriction Check (conditional)

    @Override
    protected boolean canFit(List<FantasyPlayer> team) {
        Map<String, Integer> slots = new HashMap<>(getRequiredSlots());
        slots.put("BENCH", 2); // Add Bench Slots
        return backtrack(team, 0, slots);
    }

    @Override
    protected boolean backtrack(List<FantasyPlayer> team, int index, Map<String, Integer> slots) {
        if (index == team.size()) {
            return true;
        }

        FantasyPlayer p = team.get(index);
        List<String> possiblePositions = parsePositions(p.getPosition());

        boolean isPitcher = possiblePositions.stream().anyMatch(pos -> pos.equals("SP") || pos.equals("RP") || pos.equals("CL"));
        if (!isPitcher) {
            possiblePositions.add("DH");
        }

        // Allow any player to go to BENCH
        possiblePositions.add("BENCH");

        for (String pos : possiblePositions) {
            if (slots.getOrDefault(pos, 0) > 0) {
                slots.put(pos, slots.get(pos) - 1);
                if (backtrack(team, index + 1, slots)) {
                    return true;
                }
                slots.put(pos, slots.get(pos) + 1);
            }
        }

        return false;
    }
}
