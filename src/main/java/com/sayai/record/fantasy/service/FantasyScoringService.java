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

        int size = scores.size();
        List<FantasyRotisserieScore> validScores = new ArrayList<>(size);

        for (int i = 0; i < size; i++) {
            FantasyRotisserieScore s = scores.get(i);
            if (valueExtractor.apply(s) != null) {
                validScores.add(s);
            } else {
                rankSetter.accept(s, totalParticipants);
                pointSetter.accept(s, 0.0);
            }
        }

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
        int validSize = validScores.size();
        int i = 0;
        while (i < validSize) {
            int j = i;
            T currentVal = valueExtractor.apply(validScores.get(i));

            // Find end of tie group
            while (j < validSize && valueExtractor.apply(validScores.get(j)).equals(currentVal)) {
                j++;
            }
            // Group is from i to j-1
            int count = j - i;
            int startRank = i + 1; // 1-based rank

            double avgPoints;
            if (count == 1) {
                avgPoints = (totalParticipants - startRank + 1) * 10.0;
            } else {
                double firstPts = (totalParticipants - startRank + 1) * 10.0;
                double lastPts = (totalParticipants - (startRank + count - 1) + 1) * 10.0;
                avgPoints = (firstPts + lastPts) / 2.0;
            }

            // Assign
            for (int k = i; k < j; k++) {
                FantasyRotisserieScore s = validScores.get(k);
                rankSetter.accept(s, startRank); // Everyone gets the best rank (e.g. T-3rd)
                pointSetter.accept(s, avgPoints);
            }

            i = j;
        }
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
