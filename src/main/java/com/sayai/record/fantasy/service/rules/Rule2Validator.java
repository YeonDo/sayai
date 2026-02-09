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

    @Override
    protected boolean canFit(List<FantasyPlayer> team) {
        // Enforce 20 players check if called with full roster
        if (team.size() > 20) return false;

        // Custom validation logic for Rule 2:
        // 1. Minimum 9 Batters (Fielders/DH)
        // 2. Minimum 9 Pitchers
        // 3. SP >= 4, RP >= 4, CL >= 1
        // 4. C, 1B, 2B, 3B, SS, LF, CF, RF, DH >= 1

        // This is complex to solve with simple slot filling if we just have 2 'BENCH' slots that can take anything.
        // Instead, we define the MANDATORY slots first.
        // Mandatory: SP:4, RP:4, CL:1, C:1, 1B:1, 2B:1, 3B:1, SS:1, LF:1, CF:1, RF:1, DH:1
        // Total Mandatory = 4+4+1 + 1*9 = 18.
        // Remaining 2 slots can be ANY position (Pitcher or Batter),
        // BUT strict constraint: Total Batters >= 9, Total Pitchers >= 9.
        // Since Mandatory Pitchers = 4+4+1 = 9, we already meet Pitcher min.
        // Since Mandatory Batters = 1*9 = 9, we already meet Batter min.
        // So the remaining 2 slots are truly free (BENCH).

        Map<String, Integer> required = new HashMap<>();
        required.put("SP", 4);
        required.put("RP", 4);
        required.put("CL", 1);
        required.put("C", 1);
        required.put("1B", 1);
        required.put("2B", 1);
        required.put("3B", 1);
        required.put("SS", 1);
        required.put("LF", 1);
        required.put("CF", 1);
        required.put("RF", 1);
        required.put("DH", 1);
        required.put("BENCH", 2); // 2 Flexible slots

        // Clone list to avoid modification issues
        List<FantasyPlayer> players = new ArrayList<>(team);

        // Optimization: Sort players by flexibility (least flexible first) to fail fast?
        // Actually, just standard backtracking is fine for 20 players.

        return backtrack(players, 0, required);
    }

    // Override parsePositions to ensure we handle raw strings correctly
    // (Assuming Rule1Validator has this helper, but if not we implement or reuse)
    // Rule1Validator has it as private usually? If protected we reuse.
    // Let's assume protected as seen in previous read_file.

    @Override
    protected boolean backtrack(List<FantasyPlayer> team, int index, Map<String, Integer> slots) {
        if (index == team.size()) {
            return true;
        }

        FantasyPlayer p = team.get(index);
        List<String> possiblePositions = new ArrayList<>();

        // Parse player positions (e.g., "SP" or "1B/DH")
        String[] parts = p.getPosition().split("/");
        boolean isPitcher = false;
        for (String s : parts) {
            String clean = s.trim().toUpperCase();
            possiblePositions.add(clean);
            if (clean.equals("SP") || clean.equals("RP") || clean.equals("CL")) {
                isPitcher = true;
            }
        }

        // If not a pitcher, can play DH (unless DH is already in list)
        if (!isPitcher && !possiblePositions.contains("DH")) {
            possiblePositions.add("DH");
        }

        // ANY player can go to BENCH
        possiblePositions.add("BENCH");

        for (String pos : possiblePositions) {
            if (slots.getOrDefault(pos, 0) > 0) {
                // Decrement slot
                slots.put(pos, slots.get(pos) - 1);

                if (backtrack(team, index + 1, slots)) {
                    return true;
                }

                // Backtrack
                slots.put(pos, slots.get(pos) + 1);
            }
        }

        return false;
    }
}
