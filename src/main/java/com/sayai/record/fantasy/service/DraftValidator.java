package com.sayai.record.fantasy.service;

import com.sayai.record.fantasy.entity.FantasyGame;
import com.sayai.record.fantasy.entity.FantasyParticipant;
import com.sayai.record.fantasy.entity.FantasyPlayer;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

@Component
public class DraftValidator {

    // Slot definitions
    private static final Map<String, Integer> REQUIRED_SLOTS = new HashMap<>();

    static {
        REQUIRED_SLOTS.put("C", 1);
        REQUIRED_SLOTS.put("1B", 1);
        REQUIRED_SLOTS.put("2B", 1);
        REQUIRED_SLOTS.put("SS", 1);
        REQUIRED_SLOTS.put("3B", 1);
        REQUIRED_SLOTS.put("LF", 1);
        REQUIRED_SLOTS.put("CF", 1);
        REQUIRED_SLOTS.put("RF", 1);
        REQUIRED_SLOTS.put("DH", 1); // Any Hitter
        REQUIRED_SLOTS.put("SP", 4);
        REQUIRED_SLOTS.put("RP", 4);
        REQUIRED_SLOTS.put("CL", 1);
    }

    public void validate(FantasyGame game, FantasyPlayer newPlayer, List<FantasyPlayer> currentTeam, FantasyParticipant participant) {
        // Rule 2 Checks
        if (game.getRuleType() == FantasyGame.RuleType.RULE_2) {
            if (currentTeam.isEmpty()) {
                if (participant == null || participant.getPreferredTeam() == null) {
                    throw new IllegalStateException("Preferred team not set for participant");
                }
                // Check if newPlayer.team matches preferredTeam
                // Assuming simple string comparison. Normalize if necessary.
                if (!participant.getPreferredTeam().equalsIgnoreCase(newPlayer.getTeam())) {
                    throw new IllegalStateException("First pick must be from preferred team: " + participant.getPreferredTeam());
                }
            }
        }

        // Rule 1 Checks (Composition)
        // Combine current team and new player to check if they CAN fit
        List<FantasyPlayer> combinedTeam = new ArrayList<>(currentTeam);
        combinedTeam.add(newPlayer);

        if (!canFit(combinedTeam)) {
            throw new IllegalStateException("Drafting this player violates roster composition rules.");
        }
    }

    private boolean canFit(List<FantasyPlayer> team) {
        // Deep copy of slots to track usage in recursion
        Map<String, Integer> slots = new HashMap<>(REQUIRED_SLOTS);
        return backtrack(team, 0, slots);
    }

    private boolean backtrack(List<FantasyPlayer> team, int index, Map<String, Integer> slots) {
        if (index == team.size()) {
            return true; // All players fitted
        }

        FantasyPlayer p = team.get(index);
        List<String> possiblePositions = parsePositions(p.getPosition());

        // If player is NOT a pitcher, they can also be DH
        boolean isPitcher = possiblePositions.stream().anyMatch(pos -> pos.equals("SP") || pos.equals("RP") || pos.equals("CL"));
        if (!isPitcher) {
            // Add DH as a possibility for hitters
            // Note: If a player is "1B", they can be 1B or DH.
            // We append DH to the list of possibilities check.
            // However, we should verify if "P" exists in our logic.
            // Based on slots, we have SP, RP, CL.
            // We'll assume anyone who doesn't map *exclusively* to SP/RP/CL is a hitter candidate.
            // Actually, simpler: Try to fit in specific position OR DH.
            possiblePositions.add("DH");
        }

        for (String pos : possiblePositions) {
            if (slots.getOrDefault(pos, 0) > 0) {
                // Try this slot
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

    private List<String> parsePositions(String positionString) {
        if (positionString == null || positionString.isEmpty()) return Collections.emptyList();

        // Split by comma and trim
        // Example: "1B, RF" -> ["1B", "RF"]
        return Arrays.stream(positionString.split(","))
                .map(String::trim)
                .collect(Collectors.toList());
    }
}
