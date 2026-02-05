package com.sayai.record.fantasy.service.rules;

import com.sayai.record.fantasy.entity.FantasyGame;
import com.sayai.record.fantasy.entity.FantasyParticipant;
import com.sayai.record.fantasy.entity.FantasyPlayer;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

@Component
public class Rule1Validator implements DraftRuleValidator {

    private static final int MAX_TYPE1_FOREIGNERS = 3;
    private static final int MAX_TYPE2_FOREIGNERS = 1;

    private static final Map<String, Integer> BASE_SLOTS = new HashMap<>();

    static {
        BASE_SLOTS.put("C", 1);
        BASE_SLOTS.put("1B", 1);
        BASE_SLOTS.put("2B", 1);
        BASE_SLOTS.put("SS", 1);
        BASE_SLOTS.put("3B", 1);
        BASE_SLOTS.put("LF", 1);
        BASE_SLOTS.put("CF", 1);
        BASE_SLOTS.put("RF", 1);
        BASE_SLOTS.put("DH", 1);
        BASE_SLOTS.put("SP", 4);
        BASE_SLOTS.put("RP", 4);
        BASE_SLOTS.put("CL", 1);
    }

    @Override
    public FantasyGame.RuleType getSupportedRuleType() {
        return FantasyGame.RuleType.RULE_1;
    }

    @Override
    public int getTotalPlayerCount() {
        return 18;
    }

    protected Map<String, Integer> getRequiredSlots() {
        return BASE_SLOTS;
    }

    @Override
    public void validate(FantasyGame game, FantasyPlayer newPlayer, List<FantasyPlayer> currentTeam, FantasyParticipant participant) {
        // Rule 1 Checks (Composition)
        List<FantasyPlayer> combinedTeam = new ArrayList<>(currentTeam);
        combinedTeam.add(newPlayer);

        if (!canFit(combinedTeam)) {
            throw new IllegalStateException("Drafting this player violates roster composition rules.");
        }

        // Foreigner Limits Check
        validateForeignerLimits(combinedTeam);
    }

    private void validateForeignerLimits(List<FantasyPlayer> team) {
        long type1Count = team.stream().filter(p -> {
            FantasyPlayer.ForeignerType type = Optional.ofNullable(p.getForeignerType()).orElse(FantasyPlayer.ForeignerType.NONE);
            return type == FantasyPlayer.ForeignerType.TYPE_1;
        }).count();

        long type2Count = team.stream().filter(p -> {
            FantasyPlayer.ForeignerType type = Optional.ofNullable(p.getForeignerType()).orElse(FantasyPlayer.ForeignerType.NONE);
            return type == FantasyPlayer.ForeignerType.TYPE_2;
        }).count();

        if (type1Count > MAX_TYPE1_FOREIGNERS) {
            throw new IllegalStateException("Cannot draft more than " + MAX_TYPE1_FOREIGNERS + " Foreigners (TYPE_1).");
        }
        if (type2Count > MAX_TYPE2_FOREIGNERS) {
            throw new IllegalStateException("Cannot draft more than " + MAX_TYPE2_FOREIGNERS + " Asian Quarter (TYPE_2).");
        }
    }

    private boolean canFit(List<FantasyPlayer> team) {
        Map<String, Integer> slots = new HashMap<>(getRequiredSlots());
        return backtrack(team, 0, slots);
    }

    private boolean backtrack(List<FantasyPlayer> team, int index, Map<String, Integer> slots) {
        if (index == team.size()) {
            return true;
        }

        FantasyPlayer p = team.get(index);
        List<String> possiblePositions = parsePositions(p.getPosition());

        boolean isPitcher = possiblePositions.stream().anyMatch(pos -> pos.equals("SP") || pos.equals("RP") || pos.equals("CL"));
        if (!isPitcher) {
            possiblePositions.add("DH");
        }

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

    private List<String> parsePositions(String positionString) {
        if (positionString == null || positionString.isEmpty()) return Collections.emptyList();
        return Arrays.stream(positionString.split(","))
                .map(String::trim)
                .collect(Collectors.toList());
    }
}
