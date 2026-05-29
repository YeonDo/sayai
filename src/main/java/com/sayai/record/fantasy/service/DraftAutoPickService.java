package com.sayai.record.fantasy.service;

import com.sayai.kbo.model.KboHitterStats;
import com.sayai.kbo.model.KboPitcherStats;
import com.sayai.kbo.repository.KboHitterStatsRepository;
import com.sayai.kbo.repository.KboPitcherStatsRepository;
import com.sayai.record.fantasy.dto.DraftRequest;
import com.sayai.record.fantasy.entity.*;
import com.sayai.record.fantasy.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class DraftAutoPickService {

    private final FantasyPlayerRepository fantasyPlayerRepository;
    private final DraftPickRepository draftPickRepository;
    private final FantasyGameRepository fantasyGameRepository;
    private final FantasyParticipantRepository fantasyParticipantRepository;
    private final DraftPickExecutor draftPickExecutor;
    private final DraftValidator draftValidator;
    private final KboHitterStatsRepository kboHitterStatsRepository;
    private final KboPitcherStatsRepository kboPitcherStatsRepository;

    // BotPickTrigger의 @EventListener에서 비동기 진입점으로 호출
    @Async
    public void autoPickAsync(Long gameSeq) {
        try {
            autoPick(gameSeq);
        } catch (Exception e) {
            log.error("Error in autoPick for game {}: {}", gameSeq, e.getMessage());
        }
    }

    @Transactional
    public void autoPick(Long gameSeq) {
        autoPick(gameSeq, false);
    }

    /**
     * @param skipDeadlineCheck true이면 nextPickDeadline 체크를 건너뜀.
     *                          BotPickTrigger에서 봇 차례임이 확실한 경우에 사용.
     *                          DraftScheduler의 타임아웃 감지 경로는 false로 호출.
     */
    @Transactional
    public void autoPick(Long gameSeq, boolean skipDeadlineCheck) {
        FantasyGame game = fantasyGameRepository.findById(gameSeq)
                .orElseThrow(() -> new IllegalArgumentException("Invalid Game Seq"));

        if (game.getStatus() != FantasyGame.GameStatus.DRAFTING) {
            return;
        }

        // 아직 픽 마감 시간이 남아있으면 스케줄러 경로에서만 조기 종료
        if (!skipDeadlineCheck && game.getNextPickDeadline() != null
                && game.getNextPickDeadline().isAfter(LocalDateTime.now())) {
            return;
        }

        DraftPickExecutor.NextPickInfo nextPick = draftPickExecutor.getNextPickInfo(game);
        Long playerId = nextPick.pickerId;

        List<DraftPick> picks = draftPickRepository.findByFantasyGameSeq(gameSeq);
        Set<Long> pickedPlayerSeqs = picks.stream()
                .map(DraftPick::getFantasyPlayerSeq)
                .collect(Collectors.toSet());

        // 이미 픽된 선수를 제외한 가용 선수 목록
        List<FantasyPlayer> available;
        if (pickedPlayerSeqs.isEmpty()) {
            available = fantasyPlayerRepository.findAllActivePlayers();
        } else {
            available = fantasyPlayerRepository.findBySeqNotIn(pickedPlayerSeqs);
        }

        // 봇 참가자는 DB에 없을 수 있으므로 null-safe 처리 (preferredTeam 등 속성 접근용)
        FantasyParticipant participant = fantasyParticipantRepository
                .findByFantasyGameSeqAndMemberId(gameSeq, playerId).orElse(null);
        if (participant == null) participant = FantasyParticipant.builder().memberId(playerId).build();

        // 해당 픽커의 기존 픽 목록 (포지션 슬롯 체크에 사용)
        List<DraftPick> userPicks = picks.stream()
                .filter(p -> p.getMemberId().equals(playerId))
                .collect(Collectors.toList());

        // 현재 로스터 기준 선발 가능한 포지션 슬롯이 있는 선수 (봇/유저 공통)
        List<FantasyPlayer> slottable = available.stream()
                .filter(p -> draftPickExecutor.hasOpenSlot(userPicks, p))
                .collect(Collectors.toList());
        List<FantasyPlayer> pool = slottable.isEmpty() ? available : slottable;

        List<FantasyPlayer> candidates;
        if (nextPick.isBot) {
            // 1라운드 1차 지명 룰 적용 여부
            boolean isFirstPickRound = Boolean.TRUE.equals(game.getUseFirstPickRule()) && nextPick.round == 1;

            if (isFirstPickRound) {
                // 1라운드: 선호팀 선수 중 p_rank 가장 높은 선수 선발
                List<FantasyPlayer> prefPool = pool;
                String pref = participant.getPreferredTeam();
                if (pref != null) {
                    String prefLower = pref.trim().toLowerCase();
                    List<FantasyPlayer> filtered = pool.stream()
                            .filter(p -> p.getTeam().toLowerCase().contains(prefLower)
                                    || prefLower.contains(p.getTeam().toLowerCase()))
                            .collect(Collectors.toList());
                    if (!filtered.isEmpty()) prefPool = filtered;
                }
                candidates = buildRankSortedCandidates(prefPool);
            } else {
                // 2라운드 이후: slottable 내 p_rank 상위 20 (투수 10, 타자 10) 중 무작위
                candidates = buildBotCandidates(pool);
                Collections.shuffle(candidates);
            }
        } else {
            // 유저 오토픽: 선발 가능한 포지션 선수 중 무작위
            candidates = pool;
            if (Boolean.TRUE.equals(game.getUseFirstPickRule()) && nextPick.round == 1) {
                String pref = participant.getPreferredTeam();
                if (pref != null) {
                    String prefLower = pref.trim().toLowerCase();
                    List<FantasyPlayer> prefCandidates = pool.stream()
                            .filter(p -> p.getTeam().toLowerCase().contains(prefLower)
                                    || prefLower.contains(p.getTeam().toLowerCase()))
                            .collect(Collectors.toList());
                    if (!prefCandidates.isEmpty()) candidates = prefCandidates;
                }
            }
            Collections.shuffle(candidates);
        }

        // 샐캡·드래프트 규칙 검증을 위해 현재 팀 구성 조회
        Set<Long> userPickedSeqs = userPicks.stream().map(DraftPick::getFantasyPlayerSeq).collect(Collectors.toSet());
        List<FantasyPlayer> currentTeam = userPickedSeqs.isEmpty()
                ? Collections.emptyList()
                : fantasyPlayerRepository.findAllById(userPickedSeqs);

        final FantasyParticipant finalParticipant = participant;
        FantasyPlayer selected = selectCandidate(game, finalParticipant, candidates, currentTeam);

        // 봇 후보가 전부 검증 실패하면 전체 가용 선수 대상으로 재시도
        if (selected == null && nextPick.isBot) {
            List<FantasyPlayer> allShuffled = new ArrayList<>(available);
            Collections.shuffle(allShuffled);
            selected = selectCandidate(game, finalParticipant, allShuffled, currentTeam);
        }

        if (selected != null) {
            DraftRequest req = new DraftRequest();
            req.setFantasyGameSeq(gameSeq);
            req.setFantasyPlayerSeq(selected.getSeq());
            req.setMemberId(playerId);
            req.setAutoPick(true);
            draftPickExecutor.commitPick(game, req, selected, true, nextPick);
        } else {
            log.error("AutoPick failed: No valid player found for game {} user {}", gameSeq, playerId);
        }
    }

    /**
     * 후보 목록을 앞에서부터 순회하며 샐캡·드래프트 규칙을 통과하는 첫 번째 선수를 반환.
     * 검증 실패 시 예외를 삼키고 다음 후보로 넘어간다.
     */
    private FantasyPlayer selectCandidate(FantasyGame game, FantasyParticipant participant,
                                          List<FantasyPlayer> candidates, List<FantasyPlayer> currentTeam) {
        for (FantasyPlayer p : candidates) {
            try {
                if (game.getSalaryCap() != null && game.getSalaryCap() > 0) {
                    List<FantasyPlayer> hypotheticalTeam = new ArrayList<>(currentTeam);
                    hypotheticalTeam.add(p);
                    int projectedCost = com.sayai.record.fantasy.util.SalaryCapCalculator
                            .calculateTeamCost(game, participant, hypotheticalTeam).getTotalCost();
                    if (projectedCost > game.getSalaryCap()) continue;
                }
                draftValidator.validate(game, p, currentTeam, participant);
                return p;
            } catch (Exception e) {
                // invalid, try next
            }
        }
        return null;
    }

    /**
     * 1라운드 봇 1차 지명용: pool 내 선수를 p_rank 내림차순으로 정렬해 반환.
     * p_rank 데이터가 없는 선수는 0.0으로 처리되어 목록 뒤로 밀린다.
     * 상위 N개 제한 없이 전체를 반환하므로 selectCandidate가 앞에서부터 순서대로 시도한다.
     */
    private List<FantasyPlayer> buildRankSortedCandidates(List<FantasyPlayer> pool) {
        int currentSeason = LocalDate.now().getYear();
        Set<Long> seqs = pool.stream().map(FantasyPlayer::getSeq).collect(Collectors.toSet());

        Map<Long, Double> rankMap = new java.util.HashMap<>();
        kboHitterStatsRepository.findByPlayerIdInAndSeason(seqs, currentSeason)
                .stream().filter(s -> s.getPRank() != null)
                .forEach(s -> rankMap.put(s.getPlayerId(), s.getPRank()));
        kboPitcherStatsRepository.findByPlayerIdInAndSeason(seqs, currentSeason)
                .stream().filter(s -> s.getPRank() != null)
                .forEach(s -> rankMap.put(s.getPlayerId(), s.getPRank()));

        return pool.stream()
                .sorted(Comparator.comparingDouble(
                        (FantasyPlayer p) -> rankMap.getOrDefault(p.getSeq(), 0.0)).reversed())
                .collect(Collectors.toList());
    }

    /**
     * 2라운드 이후 봇 픽용: pool에서 p_rank 상위 10타자 + 상위 10투수를 후보로 선정.
     * p_rank 있는 선수가 아무도 없으면 pool 전체를 그대로 반환.
     */
    private List<FantasyPlayer> buildBotCandidates(List<FantasyPlayer> available) {
        int currentSeason = LocalDate.now().getYear();
        Set<Long> availableSeqs = available.stream().map(FantasyPlayer::getSeq).collect(Collectors.toSet());

        Set<Long> topHitterSeqs = kboHitterStatsRepository
                .findByPlayerIdInAndSeason(availableSeqs, currentSeason)
                .stream()
                .filter(s -> s.getPRank() != null)
                .sorted(Comparator.comparingDouble(KboHitterStats::getPRank).reversed())
                .limit(10)
                .map(KboHitterStats::getPlayerId)
                .collect(Collectors.toSet());

        Set<Long> topPitcherSeqs = kboPitcherStatsRepository
                .findByPlayerIdInAndSeason(availableSeqs, currentSeason)
                .stream()
                .filter(s -> s.getPRank() != null)
                .sorted(Comparator.comparingDouble(KboPitcherStats::getPRank).reversed())
                .limit(10)
                .map(KboPitcherStats::getPlayerId)
                .collect(Collectors.toSet());

        List<FantasyPlayer> botCandidates = available.stream()
                .filter(p -> topHitterSeqs.contains(p.getSeq()) || topPitcherSeqs.contains(p.getSeq()))
                .collect(Collectors.toList());

        return botCandidates.isEmpty() ? available : botCandidates;
    }
}
