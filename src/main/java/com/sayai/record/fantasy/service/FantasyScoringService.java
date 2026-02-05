package com.sayai.record.fantasy.service;

import com.sayai.record.fantasy.dto.FantasyScoreDto;
import com.sayai.record.fantasy.entity.FantasyRotisserieScore;
import com.sayai.record.fantasy.repository.FantasyRotisserieScoreRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class FantasyScoringService {

    private final FantasyRotisserieScoreRepository scoreRepository;

    @Transactional(readOnly = true)
    public List<FantasyScoreDto> getScores(Long gameSeq, Integer round) {
        List<FantasyRotisserieScore> scores = scoreRepository.findByFantasyGameSeqAndRound(gameSeq, round);
        return scores.stream().map(this::convertToDto).collect(Collectors.toList());
    }

    @Transactional
    public void saveAndCalculateScores(Long gameSeq, Integer round, List<FantasyScoreDto> inputScores) {
        // 1. Bulk Fetch Existing Scores
        Map<Long, FantasyRotisserieScore> existingScores = scoreRepository.findByFantasyGameSeqAndRound(gameSeq, round)
                .stream()
                .collect(Collectors.toMap(FantasyRotisserieScore::getPlayerId, Function.identity()));

        List<FantasyRotisserieScore> entities = new ArrayList<>();

        for (FantasyScoreDto dto : inputScores) {
            FantasyRotisserieScore entity = existingScores.get(dto.getPlayerId());

            if (entity == null) {
                entity = FantasyRotisserieScore.builder()
                        .fantasyGameSeq(gameSeq)
                        .playerId(dto.getPlayerId())
                        .round(round)
                        .build();
            }

            // Update stats
            entity.setAvg(dto.getAvg());
            entity.setRbi(dto.getRbi());
            entity.setHr(dto.getHr());
            entity.setSoBatter(dto.getSoBatter());
            entity.setSb(dto.getSb());
            entity.setWins(dto.getWins());
            entity.setEra(dto.getEra());
            entity.setSoPitcher(dto.getSoPitcher());
            entity.setWhip(dto.getWhip());
            entity.setSaves(dto.getSaves());

            entities.add(entity);
        }

        // 2. Calculate Ranks and Points
        if (!entities.isEmpty()) {
            calculateRotisseriePoints(entities);
        }

        scoreRepository.saveAll(entities);
    }

    private void calculateRotisseriePoints(List<FantasyRotisserieScore> scores) {
        int n = scores.size();

        // Helper to calculate points for a category
        // asc = true means lower is better (ERA, WHIP, Batter K?)
        // Wait, Batter K (Strikeouts): usually for batters, higher is NOT better?
        // User request: "삼진 , 방어율 , WHIP 는 낮은 순대로 높은 순위야"
        // So: Batter SO (Low is better), ERA (Low is better), WHIP (Low is better).
        // Others (AVG, RBI, HR, SB, Wins, Pitcher SO, Saves): High is better.

        assignPoints(scores, FantasyRotisserieScore::getAvg, FantasyRotisserieScore::setRankAvg, FantasyRotisserieScore::setPointsAvg, false, n);
        assignPoints(scores, FantasyRotisserieScore::getRbi, FantasyRotisserieScore::setRankRbi, FantasyRotisserieScore::setPointsRbi, false, n);
        assignPoints(scores, FantasyRotisserieScore::getHr, FantasyRotisserieScore::setRankHr, FantasyRotisserieScore::setPointsHr, false, n);
        assignPoints(scores, FantasyRotisserieScore::getSoBatter, FantasyRotisserieScore::setRankSoBatter, FantasyRotisserieScore::setPointsSoBatter, true, n); // Low is better
        assignPoints(scores, FantasyRotisserieScore::getSb, FantasyRotisserieScore::setRankSb, FantasyRotisserieScore::setPointsSb, false, n);

        assignPoints(scores, FantasyRotisserieScore::getWins, FantasyRotisserieScore::setRankWins, FantasyRotisserieScore::setPointsWins, false, n);
        assignPoints(scores, FantasyRotisserieScore::getEra, FantasyRotisserieScore::setRankEra, FantasyRotisserieScore::setPointsEra, true, n); // Low is better
        assignPoints(scores, FantasyRotisserieScore::getSoPitcher, FantasyRotisserieScore::setRankSoPitcher, FantasyRotisserieScore::setPointsSoPitcher, false, n); // High is better (Pitcher K)
        assignPoints(scores, FantasyRotisserieScore::getWhip, FantasyRotisserieScore::setRankWhip, FantasyRotisserieScore::setPointsWhip, true, n); // Low is better
        assignPoints(scores, FantasyRotisserieScore::getSaves, FantasyRotisserieScore::setRankSaves, FantasyRotisserieScore::setPointsSaves, false, n);

        // Sum Total
        for (FantasyRotisserieScore s : scores) {
            double total = (s.getPointsAvg() != null ? s.getPointsAvg() : 0) +
                           (s.getPointsRbi() != null ? s.getPointsRbi() : 0) +
                           (s.getPointsHr() != null ? s.getPointsHr() : 0) +
                           (s.getPointsSoBatter() != null ? s.getPointsSoBatter() : 0) +
                           (s.getPointsSb() != null ? s.getPointsSb() : 0) +
                           (s.getPointsWins() != null ? s.getPointsWins() : 0) +
                           (s.getPointsEra() != null ? s.getPointsEra() : 0) +
                           (s.getPointsSoPitcher() != null ? s.getPointsSoPitcher() : 0) +
                           (s.getPointsWhip() != null ? s.getPointsWhip() : 0) +
                           (s.getPointsSaves() != null ? s.getPointsSaves() : 0);
            s.setTotalPoints(total);
        }
    }

    private <T extends Comparable<T>> void assignPoints(
            List<FantasyRotisserieScore> scores,
            Function<FantasyRotisserieScore, T> valueExtractor,
            java.util.function.BiConsumer<FantasyRotisserieScore, Integer> rankSetter,
            java.util.function.BiConsumer<FantasyRotisserieScore, Double> pointSetter,
            boolean lowerIsBetter,
            int totalParticipants) {

        // Filter out nulls
        List<FantasyRotisserieScore> validScores = scores.stream()
                .filter(s -> valueExtractor.apply(s) != null)
                .collect(Collectors.toList());

        // Sort
        validScores.sort((s1, s2) -> {
            T v1 = valueExtractor.apply(s1);
            T v2 = valueExtractor.apply(s2);
            if (lowerIsBetter) {
                return v1.compareTo(v2); // Ascending (Lower is first/better)
            } else {
                return v2.compareTo(v1); // Descending (Higher is first/better)
            }
        });

        // Assign Ranks and Points with Tie Handling
        // Rank 1 gets N * 10 points. Rank N gets 1 * 10 points.
        // Wait, request says: 1st = N*10, Last = 10.
        // If N=10: 1st=100, 2nd=90 ... 10th=10.
        // Formula: Points = (N - Rank + 1) * 10.

        // Tie handling: sum points for tied positions and divide.
        // e.g. 3rd and 4th tied.
        // 3rd points: (10-3+1)*10 = 80.
        // 4th points: (10-4+1)*10 = 70.
        // Average: (80+70)/2 = 75.

        int i = 0;
        while (i < validScores.size()) {
            int j = i;
            T currentVal = valueExtractor.apply(validScores.get(i));

            // Find end of tie group
            while (j < validScores.size() && valueExtractor.apply(validScores.get(j)).equals(currentVal)) {
                j++;
            }
            // Group is from i to j-1
            int count = j - i;
            int startRank = i + 1; // 1-based rank

            // Calculate total points for this block
            double sumPoints = 0;
            for (int r = 0; r < count; r++) {
                int currentRank = startRank + r;
                // Points for this rank position
                // Note: 'totalParticipants' should be N.
                // Assuming all participants are in the list.
                // If some have null stats, they don't get points (or 0).
                // Let's assume we grade based on N = totalParticipants even if some are invalid?
                // Or N = validScores.size()?
                // Usually N is fixed game size. But let's use validScores.size() to be fair among those who played?
                // Or better, passed 'totalParticipants' (n).

                double pts = (totalParticipants - currentRank + 1) * 10.0;
                sumPoints += pts;
            }

            double avgPoints = sumPoints / count;

            // Assign
            for (int k = i; k < j; k++) {
                FantasyRotisserieScore s = validScores.get(k);
                rankSetter.accept(s, startRank); // Everyone gets the best rank (e.g. T-3rd)
                pointSetter.accept(s, avgPoints);
            }

            i = j;
        }

        // Handle nulls (optional: set 0 points and last rank?)
        // Currently they just stay null.
    }

    private FantasyScoreDto convertToDto(FantasyRotisserieScore entity) {
        return FantasyScoreDto.builder()
                .seq(entity.getSeq())
                .fantasyGameSeq(entity.getFantasyGameSeq())
                .playerId(entity.getPlayerId())
                .round(entity.getRound())
                .avg(entity.getAvg())
                .rbi(entity.getRbi())
                .hr(entity.getHr())
                .soBatter(entity.getSoBatter())
                .sb(entity.getSb())
                .wins(entity.getWins())
                .era(entity.getEra())
                .soPitcher(entity.getSoPitcher())
                .whip(entity.getWhip())
                .saves(entity.getSaves())
                .totalPoints(entity.getTotalPoints())
                .build();
    }
}
