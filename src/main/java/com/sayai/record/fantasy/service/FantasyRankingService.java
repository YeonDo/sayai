package com.sayai.record.fantasy.service;

import com.sayai.record.dto.PitcherDto;
import com.sayai.record.dto.PlayerDto;
import com.sayai.record.fantasy.dto.FantasyScoreDto;
import com.sayai.record.fantasy.dto.ParticipantStatsDto;
import com.sayai.record.fantasy.dto.RankingTableDto;
import com.sayai.record.fantasy.entity.DraftPick;
import com.sayai.record.fantasy.entity.FantasyGame;
import com.sayai.record.fantasy.entity.FantasyParticipant;
import com.sayai.record.fantasy.entity.FantasyPlayer;
import com.sayai.record.fantasy.entity.FantasyRotisserieScore;
import com.sayai.record.fantasy.repository.*;
import com.sayai.record.model.Player;
import com.sayai.record.repository.PitchRepository;
import com.sayai.record.repository.PlayerRepository;
import com.sayai.record.service.HitService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class FantasyRankingService {

    private final FantasyGameRepository fantasyGameRepository;
    private final FantasyParticipantRepository participantRepository;
    private final DraftPickRepository draftPickRepository;
    private final FantasyPlayerRepository fantasyPlayerRepository;
    private final PlayerRepository playerRepository;
    private final HitService hitService;
    private final PitchRepository pitchRepository;
    private final FantasyRotisserieScoreRepository rotisserieScoreRepository;

    public RankingTableDto getRanking(Long gameSeq) {
        FantasyGame game = fantasyGameRepository.findById(gameSeq)
                .orElseThrow(() -> new IllegalArgumentException("Invalid Game Seq"));

        RankingTableDto response = new RankingTableDto();
        response.setGameSeq(gameSeq);
        response.setScoringType(game.getScoringType().name());

        if (game.getScoringType() == FantasyGame.ScoringType.POINTS) {
            response.setRankings(Collections.emptyList());
            return response;
        }

        if (game.getScoringType() == FantasyGame.ScoringType.ROTISSERIE) {
            return getRotisserieRanking(game, response);
        }

        // Fallback for simulation (legacy logic)
        return getSimulationRanking(game, response);
    }

    private RankingTableDto getRotisserieRanking(FantasyGame game, RankingTableDto response) {
        List<FantasyParticipant> participants = participantRepository.findByFantasyGameSeq(game.getSeq());
        List<FantasyRotisserieScore> scores = rotisserieScoreRepository.findByFantasyGameSeq(game.getSeq());

        // Group by PlayerId
        Map<Long, List<FantasyRotisserieScore>> scoresByPlayer = scores.stream()
                .collect(Collectors.groupingBy(FantasyRotisserieScore::getPlayerId));

        List<ParticipantStatsDto> statsList = new ArrayList<>();

        for (FantasyParticipant fp : participants) {
            ParticipantStatsDto dto = new ParticipantStatsDto();
            dto.setParticipantId(fp.getPlayerId());
            dto.setTeamName(fp.getTeamName());

            List<FantasyRotisserieScore> myScores = scoresByPlayer.getOrDefault(fp.getPlayerId(), Collections.emptyList());

            if (myScores.isEmpty()) {
                dto.setTotalPoints(0.0);
                statsList.add(dto);
                continue;
            }

            // Aggregate
            double totalPoints = 0;
            // Sum counting stats
            long hr = 0, rbi = 0, sb = 0, soBat = 0;
            long wins = 0, soPitch = 0, saves = 0;

            // For ratios, we average them (not perfect but standard if raw data missing)
            // Or sum? Ratios shouldn't be summed. Weighted average needs denominator.
            // Since we don't have denominator, simple average is best effort.
            double sumAvg = 0, sumEra = 0, sumWhip = 0;
            int countAvg = 0, countEra = 0, countWhip = 0;

            List<FantasyScoreDto> roundDtos = new ArrayList<>();

            for (FantasyRotisserieScore s : myScores) {
                totalPoints += safeDouble(s.getTotalPoints());

                hr += safeInt(s.getHr());
                rbi += safeInt(s.getRbi());
                sb += safeInt(s.getSb());
                soBat += safeInt(s.getSoBatter());

                wins += safeInt(s.getWins());
                soPitch += safeInt(s.getSoPitcher());
                saves += safeInt(s.getSaves());

                if (s.getAvg() != null) { sumAvg += s.getAvg(); countAvg++; }
                if (s.getEra() != null) { sumEra += s.getEra(); countEra++; }
                if (s.getWhip() != null) { sumWhip += s.getWhip(); countWhip++; }

                roundDtos.add(convertToScoreDto(s));
            }

            // Sort rounds
            roundDtos.sort(Comparator.comparingInt(FantasyScoreDto::getRound));
            dto.setRounds(roundDtos);

            dto.setTotalPoints(totalPoints);
            dto.setHomeruns(hr);
            dto.setRbi((int) rbi);
            dto.setStolenBases((int) sb);
            dto.setBatterStrikeOuts(soBat);

            dto.setWins(wins);
            dto.setPitcherStrikeOuts(soPitch);
            dto.setSaves(saves);

            dto.setBattingAvg(countAvg > 0 ? sumAvg / countAvg : 0.0);
            dto.setEra(countEra > 0 ? sumEra / countEra : 0.0);
            dto.setWhip(countWhip > 0 ? sumWhip / countWhip : 0.0);

            statsList.add(dto);
        }

        // Sort by Total Points DESC
        statsList.sort((a, b) -> Double.compare(
                b.getTotalPoints() != null ? b.getTotalPoints() : 0.0,
                a.getTotalPoints() != null ? a.getTotalPoints() : 0.0
        ));

        response.setRankings(statsList);
        return response;
    }

    private RankingTableDto getSimulationRanking(FantasyGame game, RankingTableDto response) {
        Long gameSeq = game.getSeq();
        // 1. Get Participants
        List<FantasyParticipant> participants = participantRepository.findByFantasyGameSeq(gameSeq);

        // 2. Get All Picks for the game
        List<DraftPick> allPicks = draftPickRepository.findByFantasyGameSeq(gameSeq);
        Map<Long, List<DraftPick>> picksByParticipant = allPicks.stream()
                .collect(Collectors.groupingBy(DraftPick::getPlayerId));

        // 3. Pre-fetch Fantasy Players to minimize queries (Optional optimization, but good practice)
        // Since we iterate participants, we can just do it inside loop or bulk fetch.
        // Let's bulk fetch all relevant fantasy players.
        Set<Long> fantasyPlayerSeqs = allPicks.stream()
                .map(DraftPick::getFantasyPlayerSeq)
                .collect(Collectors.toSet());
        List<FantasyPlayer> fantasyPlayers = fantasyPlayerRepository.findAllById(fantasyPlayerSeqs);
        Map<Long, FantasyPlayer> fantasyPlayerMap = fantasyPlayers.stream()
                .collect(Collectors.toMap(FantasyPlayer::getSeq, fp -> fp));

        // 4. Pre-fetch Real Players (Map Name -> Player) to bridge the gap
        // Since findAll() might be large? No, KBO players are ~800. It's fine.
        // Or we can query by names. Let's fetch all active players to be safe and efficient.
        List<Player> realPlayers = playerRepository.findAll();
        // Handle potential duplicate names by keeping one (or log warning).
        // Using Name + Team might be better, but FantasyPlayer has "Team" string, Player has... linked entities.
        // Let's stick to Name for now as per plan.
        Map<String, Player> realPlayerMap = new HashMap<>();
        for (Player p : realPlayers) {
            // If duplicate, last one wins? Or we ignore?
            // PlayerRepository.findPlayerByName returns one.
            realPlayerMap.putIfAbsent(p.getName(), p);
        }

        LocalDate startDate = LocalDate.of(2026, 1, 1);
        LocalDate endDate = LocalDate.of(2026, 12, 31);

        // 5. Bulk Fetch Stats to avoid N+1
        List<PlayerDto> allHitterStats = hitService.findAllByPeriod(startDate, endDate);
        Map<Long, PlayerDto> hitterStatsMap = allHitterStats.stream()
                .collect(Collectors.toMap(PlayerDto::getId, dto -> dto, (a, b) -> a)); // Keep first if dupes

        List<PitcherDto> allPitcherStats = pitchRepository.getStatsByPeriod(startDate, endDate);
        Map<Long, PitcherDto> pitcherStatsMap = allPitcherStats.stream()
                .collect(Collectors.toMap(PitcherDto::getId, dto -> dto, (a, b) -> a));

        List<ParticipantStatsDto> statsList = new ArrayList<>();

        for (FantasyParticipant fp : participants) {
            ParticipantStatsDto dto = new ParticipantStatsDto();
            dto.setParticipantId(fp.getPlayerId());
            dto.setTeamName(fp.getTeamName());
            dto.setOwnerName(null); // Can fetch Member name if needed

            List<DraftPick> picks = picksByParticipant.getOrDefault(fp.getPlayerId(), Collections.emptyList());

            // Aggregators
            long totalHits = 0;
            long totalAB = 0;
            long totalHR = 0;
            long totalRBI = 0;
            long totalSB = 0;
            long totalBatSO = 0; // Batter Strikeouts

            long totalWins = 0;
            long totalPitchSO = 0; // Pitcher K
            long totalSaves = 0;
            long totalER = 0; // selfLossScore
            long totalOuts = 0; // inn (1/3 innings)
            long totalHitsAllowed = 0; // pHit
            long totalBBAllowed = 0; // baseOnBall

            for (DraftPick pick : picks) {
                FantasyPlayer fPlayer = fantasyPlayerMap.get(pick.getFantasyPlayerSeq());
                if (fPlayer == null) continue;

                Player realPlayer = realPlayerMap.get(fPlayer.getName());
                if (realPlayer != null) {
                    // Fetch Hitter Stats
                    PlayerDto hStats = hitterStatsMap.get(realPlayer.getId());
                    if (hStats != null) {
                        totalHits += safeLong(hStats.getTotalHits());
                        totalAB += safeLong(hStats.getAtBat());
                        totalHR += safeLong(hStats.getHomeruns());
                        totalRBI += safeInt(hStats.getRbi());
                        totalSB += safeInt(hStats.getSb());
                        totalBatSO += safeLong(hStats.getStrikeOut());
                    }

                    // Fetch Pitcher Stats
                    PitcherDto pStats = pitcherStatsMap.get(realPlayer.getId());
                    if (pStats != null) {
                        totalWins += safeLong(pStats.getWins());
                        totalPitchSO += safeLong(pStats.getStOut());
                        totalSaves += safeLong(pStats.getSaves());
                        totalER += safeLong(pStats.getSelfLossScore());
                        totalOuts += safeLong(pStats.getInn());
                        totalHitsAllowed += safeLong(pStats.getPHit());
                        totalBBAllowed += safeLong(pStats.getBaseOnBall());
                    }
                }
            }

            // Calculate Averages
            dto.setBattingAvg(totalAB > 0 ? (double) totalHits / totalAB : 0.0);
            dto.setHomeruns(totalHR);
            dto.setRbi((int) totalRBI);
            dto.setStolenBases((int) totalSB);
            dto.setBatterStrikeOuts(totalBatSO);

            dto.setWins(totalWins);
            dto.setPitcherStrikeOuts(totalPitchSO);
            dto.setSaves(totalSaves);

            // ERA = (ER * 9) / IP. IP = Outs / 3. => (ER * 27) / Outs.
            dto.setEra(totalOuts > 0 ? (double) (totalER * 27) / totalOuts : 0.0);

            // WHIP = (Hits + BB) / IP. => (Hits + BB) * 3 / Outs.
            dto.setWhip(totalOuts > 0 ? (double) (totalHitsAllowed + totalBBAllowed) * 3 / totalOuts : 0.0);

            statsList.add(dto);
        }

        response.setRankings(statsList);
        return response;
    }

    private FantasyScoreDto convertToScoreDto(FantasyRotisserieScore entity) {
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

    private double safeDouble(Double val) {
        return val == null ? 0.0 : val;
    }

    private long safeLong(Long val) {
        return val == null ? 0L : val;
    }

    private int safeInt(Integer val) {
        return val == null ? 0 : val;
    }
}
