package com.sayai.record.fantasy.util;

import com.sayai.record.fantasy.entity.FantasyGame;
import com.sayai.record.fantasy.entity.FantasyParticipant;
import com.sayai.record.fantasy.entity.FantasyPlayer;

import java.util.List;

public class SalaryCapCalculator {

    public static SalaryCapResult calculateTeamCost(FantasyGame game, FantasyParticipant participant, List<FantasyPlayer> team) {
        if (team == null || team.isEmpty()) {
            return SalaryCapResult.builder().totalCost(0).build();
        }

        int totalCost = 0;
        int maxDiscountableCost = -1;
        FantasyPlayer discountTarget = null;

        for (FantasyPlayer p : team) {
            int cost = p.getCost() == null ? 0 : p.getCost();
            totalCost += cost;

            if (game != null && Boolean.TRUE.equals(game.getUseFirstPickRule()) && participant != null) {
                if (participant.getPreferredTeam() != null && participant.getPreferredTeam().equals(p.getTeam())) {
                    if (p.getForeignerType() == FantasyPlayer.ForeignerType.NONE || p.getForeignerType() == null) {
                        if (cost > maxDiscountableCost) {
                            maxDiscountableCost = cost;
                            discountTarget = p;
                        }
                    }
                }
            }
        }

        if (discountTarget != null) {
            int originalCost = maxDiscountableCost;
            int discountedCost = (int) Math.round(originalCost / 2.0);
            totalCost = totalCost - originalCost + discountedCost;

            return SalaryCapResult.builder()
                    .totalCost(totalCost)
                    .discountedPlayerSeq(discountTarget.getSeq())
                    .originalCost(originalCost)
                    .discountedCost(discountedCost)
                    .build();
        }

        return SalaryCapResult.builder()
                .totalCost(totalCost)
                .build();
    }
}
