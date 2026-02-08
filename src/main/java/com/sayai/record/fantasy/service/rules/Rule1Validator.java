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
    private static final int MIN_REQUIRED_TEAMS = 10;

    // KBO Teams
    private static final Set<String> KBO_TEAMS = Set.of(
            "KIA", "LG", "KT", "SSG", "NC", "두산", "롯데", "삼성", "한화", "키움"
    );

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
        // Check First Pick Rule Option
        if (Boolean.TRUE.equals(game.getUseFirstPickRule())) {
            if (currentTeam.isEmpty()) {
                if (participant == null || participant.getPreferredTeam() == null) {
                    throw new IllegalStateException("Preferred team not set for participant");
                }

                String pref = participant.getPreferredTeam().trim();
                String playerTeam = newPlayer.getTeam().trim();

                boolean match = playerTeam.toLowerCase().contains(pref.toLowerCase()) ||
                        pref.toLowerCase().contains(playerTeam.toLowerCase());

                if (!match) {
                    throw new IllegalStateException("1차 지명 룰 위반: " + participant.getPreferredTeam());
                }
            }
        }

        // Rule 1 Checks (Composition)
        List<FantasyPlayer> combinedTeam = new ArrayList<>(currentTeam);
        combinedTeam.add(newPlayer);

        if (!canFit(combinedTeam)) {
            throw new IllegalStateException("이 선수를 뽑으면 로스터 포지션을 채울 수 없습니다.");
        }

        // Foreigner Limits Check
        validateForeignerLimits(combinedTeam);

        // Team Restriction Check (Optional)
        if (Boolean.TRUE.equals(game.getUseTeamRestriction())) {
            validateTeamRestriction(combinedTeam);
        }
    }

    protected void validateTeamRestriction(List<FantasyPlayer> team) {
        Set<String> distinctTeams = team.stream()
                .map(FantasyPlayer::getTeam)
                .filter(Objects::nonNull)
                .map(String::trim)
                .collect(Collectors.toSet());

        // Total roster size is determined by total slots
        int totalSlots = getTotalPlayerCount();
        int currentSize = team.size(); // After this pick

        int remainingSlots = totalSlots - currentSize;

        // Count ONLY KBO teams for the requirement? Or any distinct team?
        // Assuming user means the 10 KBO teams.
        // If the rule is strictly "pick from 10 teams", usually means cover all 10.
        // Let's identify missing KBO teams.
        Set<String> missingTeamsSet = new HashSet<>(KBO_TEAMS);
        // Normalize comparison (case insensitive or exact?)
        // Assuming data is consistent, but let's be safe if possible.
        // Actually the team names in DB are Korean/English mix as per Set.of above.
        // Let's assume exact match for now based on previous code.

        // Remove picked teams from missing set
        // Handle potential minor differences? For now exact match against KBO_TEAMS
        // If a player has "KIA Tigers", we need to know. Assuming "KIA", "LG" etc.
        // If existing logic used simple distinct count, it didn't enforce SPECIFIC teams, just count.
        // "10개 구단의 선수를 적어도 한명씩 픽해야하는 규칙" -> Must pick at least one from EACH of the 10 teams.
        // So we should track specific coverage.

        // Filter distinctTeams to only those in KBO_TEAMS to be safe
        for (String picked : distinctTeams) {
            // Check if picked matches any KBO team (contains or exact)
            // If data is clean "KIA", "LG", then remove.
            if (KBO_TEAMS.contains(picked)) {
                missingTeamsSet.remove(picked);
            } else {
                 // Try partial match if needed? "KIA Tigers" -> "KIA"
                 // If not matching, it's a team not in the list (e.g. military/indie? unlikely in KBO fantasy)
            }
        }

        int missingCount = missingTeamsSet.size();

        // If we don't have enough remaining slots to pick a new team for each missing team
        if (remainingSlots < missingCount) {
             List<String> sortedMissing = new ArrayList<>(missingTeamsSet);
             Collections.sort(sortedMissing);
             String missingStr = String.join(", ", sortedMissing);
             throw new IllegalStateException("10개 구단에서 각각 한명씩 뽑아야합니다. 빠진 팀 : [" + missingStr + "]");
        }
    }

    protected void validateForeignerLimits(List<FantasyPlayer> team) {
        long type1Count = team.stream().filter(p -> {
            FantasyPlayer.ForeignerType type = Optional.ofNullable(p.getForeignerType()).orElse(FantasyPlayer.ForeignerType.NONE);
            return type == FantasyPlayer.ForeignerType.TYPE_1;
        }).count();

        long type2Count = team.stream().filter(p -> {
            FantasyPlayer.ForeignerType type = Optional.ofNullable(p.getForeignerType()).orElse(FantasyPlayer.ForeignerType.NONE);
            return type == FantasyPlayer.ForeignerType.TYPE_2;
        }).count();

        if (type1Count > MAX_TYPE1_FOREIGNERS) {
            throw new IllegalStateException("외국인 용병 제한 " + MAX_TYPE1_FOREIGNERS + " 명을 넘게 선발할 수 없습니다.");
        }
        if (type2Count > MAX_TYPE2_FOREIGNERS) {
            throw new IllegalStateException("아시아 쿼터 제한" + MAX_TYPE2_FOREIGNERS + " 명을 넘게 선발할 수 없습니다.");
        }
    }

    protected boolean canFit(List<FantasyPlayer> team) {
        Map<String, Integer> slots = new HashMap<>(getRequiredSlots());
        return backtrack(team, 0, slots);
    }

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

    protected List<String> parsePositions(String positionString) {
        if (positionString == null || positionString.isEmpty()) return new ArrayList<>();
        return Arrays.stream(positionString.split(","))
                .map(String::trim)
                .collect(Collectors.toCollection(ArrayList::new));
    }
}
