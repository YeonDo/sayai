package com.sayai.kbo.service;

import com.sayai.kbo.model.KboHitterStats;
import com.sayai.kbo.model.KboPitcherStats;
import com.sayai.kbo.repository.KboHitterStatsRepository;
import com.sayai.kbo.repository.KboPitcherStatsRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class PRankService {

    private final KboHitterStatsRepository kboHitterStatsRepository;
    private final KboPitcherStatsRepository kboPitcherStatsRepository;

    @Transactional
    public void updatePRank(int season) {
        updateHitterPRank(season);
        updatePitcherPRank(season);
        log.info("p_rank update completed for season {}", season);
    }

    private void updateHitterPRank(int season) {
        List<KboHitterStats> allStats = kboHitterStatsRepository.findAllBySeason(season);
        for (KboHitterStats stats : allStats) {
            int games = stats.getGames() != null ? stats.getGames() : 0;
            if (games == 0) {
                stats.updatePRank(null);
                continue;
            }

            double avg  = parseDouble(stats.getAvg());
            double hr6  = stats.getHr()  * 6.0 / games;
            double rbi6 = stats.getRbi() * 6.0 / games;
            double sb6  = stats.getSb()  * 6.0 / games;
            double so6  = stats.getSo()  * 6.0 / games;

            double pRank = PRankWeight.HITTER_AVG_A  * avg  + PRankWeight.HITTER_AVG_B
                         + PRankWeight.HITTER_HR_A   * hr6  + PRankWeight.HITTER_HR_B
                         + PRankWeight.HITTER_RBI_A  * rbi6 + PRankWeight.HITTER_RBI_B
                         + PRankWeight.HITTER_SB_A   * sb6  + PRankWeight.HITTER_SB_B
                         + PRankWeight.HITTER_SO_A   * so6  + PRankWeight.HITTER_SO_B;

            stats.updatePRank(round2(pRank));
        }
        kboHitterStatsRepository.saveAll(allStats);
    }

    private void updatePitcherPRank(int season) {
        List<KboPitcherStats> allStats = kboPitcherStatsRepository.findAllBySeason(season);
        for (KboPitcherStats stats : allStats) {
            int outs = stats.getOuts() != null ? stats.getOuts() : 0;
            if (outs == 0) {
                stats.updatePRank(null);
                continue;
            }

            double era   = parseDouble(stats.getEra());
            double whip  = parseDouble(stats.getWhip());
            double win18  = stats.getWin()  * 18.0 / outs;
            double so18   = stats.getSo()   * 18.0 / outs;
            double save18 = stats.getSave() * 18.0 / outs;

            double pRank = PRankWeight.PITCHER_WIN_A  * win18  + PRankWeight.PITCHER_WIN_B
                         + PRankWeight.PITCHER_ERA_A  * era    + PRankWeight.PITCHER_ERA_B
                         + PRankWeight.PITCHER_SO_A   * so18   + PRankWeight.PITCHER_SO_B
                         + PRankWeight.PITCHER_WHIP_A * whip   + PRankWeight.PITCHER_WHIP_B
                         + PRankWeight.PITCHER_SAVE_A * save18 + PRankWeight.PITCHER_SAVE_B;

            stats.updatePRank(round2(pRank));
        }
        kboPitcherStatsRepository.saveAll(allStats);
    }

    private double parseDouble(String value) {
        if (value == null || value.isEmpty()) return 0.0;
        try {
            return Double.parseDouble(value);
        } catch (NumberFormatException e) {
            return 0.0;
        }
    }

    private double round2(double value) {
        return Math.round(value * 100.0) / 100.0;
    }
}
