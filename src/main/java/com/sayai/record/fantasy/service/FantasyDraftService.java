package com.sayai.record.fantasy.service;

import com.sayai.kbo.model.KboHitterStats;
import com.sayai.kbo.model.KboPitcherStats;
import com.sayai.kbo.repository.KboHitterStatsRepository;
import com.sayai.kbo.repository.KboPitcherStatsRepository;
import com.sayai.record.fantasy.dto.DraftRequest;
import com.sayai.record.fantasy.dto.FantasyPlayerDto;
import com.sayai.record.fantasy.dto.RosterUpdateDto;
import com.sayai.record.fantasy.entity.*;
import com.sayai.record.fantasy.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class FantasyDraftService {

    private final FantasyPlayerRepository fantasyPlayerRepository;
    private final DraftPickRepository draftPickRepository;
    private final FantasyGameRepository fantasyGameRepository;
    private final FantasyParticipantRepository fantasyParticipantRepository;
    private final RosterLogRepository rosterLogRepository;
    private final DraftValidator draftValidator;
    private final DraftPickExecutor draftPickExecutor;
    private final KboHitterStatsRepository kboHitterStatsRepository;
    private final KboPitcherStatsRepository kboPitcherStatsRepository;
    private final com.sayai.record.fantasy.service.rules.Rule1Validator rule1Validator;

    private static final Set<String> PITCHER_POSITIONS = Set.of("SP", "RP", "CL");

    @Transactional
    public void joinGame(Long gameSeq, Long playerId, String preferredTeam, String teamName) {
        FantasyGame game = fantasyGameRepository.findById(gameSeq)
                .orElseThrow(() -> new IllegalArgumentException("Invalid Game Seq"));

        if (game.getStatus() != FantasyGame.GameStatus.WAITING) {
            throw new IllegalStateException("Cannot join game. Status is " + game.getStatus());
        }

        if (fantasyParticipantRepository.findByFantasyGameSeqAndMemberId(gameSeq, playerId).isPresent()) {
            throw new IllegalStateException("이미 참여 신청을 완료했습니다");
        }

        FantasyParticipant participant = FantasyParticipant.builder()
                .fantasyGameSeq(gameSeq)
                .memberId(playerId)
                .preferredTeam(preferredTeam)
                .teamName(teamName)
                .build();

        fantasyParticipantRepository.save(participant);
    }

    @Transactional(readOnly = true)
    public List<FantasyPlayerDto> getAvailablePlayers(Long gameSeq, String team, String position, String search, String sort, String foreignerType) {
        if (team != null && team.isEmpty()) team = null;
        if (position != null && position.isEmpty()) position = null;
        if (search != null && search.isEmpty()) search = null;
        if (foreignerType != null && foreignerType.isEmpty()) foreignerType = null;

        FantasyPlayer.ForeignerType fType = null;
        if (foreignerType != null) {
            try {
                fType = FantasyPlayer.ForeignerType.valueOf(foreignerType);
            } catch (IllegalArgumentException e) {
                // Invalid enum value, treat as null
            }
        }

        Set<Long> pickedPlayerSeqs;
        if (gameSeq == null || gameSeq == 0L) {
            pickedPlayerSeqs = Collections.emptySet();
        } else {
            List<DraftPick> picks = draftPickRepository.findByFantasyGameSeq(gameSeq);
            pickedPlayerSeqs = picks.stream()
                    .map(DraftPick::getFantasyPlayerSeq)
                    .collect(Collectors.toSet());
        }

        List<FantasyPlayer> filteredPlayers = fantasyPlayerRepository.findPlayers(team, position, search, fType);

        if (sort != null) {
            if ("cost_desc".equals(sort)) {
                filteredPlayers.sort((p1, p2) -> {
                    int c1 = p1.getCost() == null ? 0 : p1.getCost();
                    int c2 = p2.getCost() == null ? 0 : p2.getCost();
                    return Integer.compare(c2, c1);
                });
            } else if ("cost_asc".equals(sort)) {
                filteredPlayers.sort((p1, p2) -> {
                    int c1 = p1.getCost() == null ? 0 : p1.getCost();
                    int c2 = p2.getCost() == null ? 0 : p2.getCost();
                    return Integer.compare(c1, c2);
                });
            }
        }

        List<FantasyPlayer> availablePlayers = filteredPlayers.stream()
                .filter(p -> !pickedPlayerSeqs.contains(p.getSeq()))
                .collect(Collectors.toList());

        int currentSeason = LocalDate.now().getYear();
        Set<Long> playerSeqs = availablePlayers.stream().map(FantasyPlayer::getSeq).collect(Collectors.toSet());

        Map<Long, KboHitterStats> hitterStatsMap = kboHitterStatsRepository
                .findByPlayerIdInAndSeason(playerSeqs, currentSeason)
                .stream()
                .collect(Collectors.toMap(KboHitterStats::getPlayerId, Function.identity()));

        Map<Long, KboPitcherStats> pitcherStatsMap = kboPitcherStatsRepository
                .findByPlayerIdInAndSeason(playerSeqs, currentSeason)
                .stream()
                .collect(Collectors.toMap(KboPitcherStats::getPlayerId, Function.identity()));

        return availablePlayers.stream()
                .map(p -> {
                    FantasyPlayerDto dto = FantasyPlayerDto.from(p);
                    String primaryPos = p.getPosition() != null ? p.getPosition().split(",")[0].trim() : "";
                    if (PITCHER_POSITIONS.contains(primaryPos)) {
                        KboPitcherStats ps = pitcherStatsMap.get(p.getSeq());
                        if (ps != null) {
                            dto.setStats(ps.getWin() + "승, " + ps.getEra() + " ERA, " + ps.getWhip() + " WHIP");
                        }
                    } else {
                        KboHitterStats hs = hitterStatsMap.get(p.getSeq());
                        if (hs != null) {
                            dto.setStats(hs.getAvg() + ", " + hs.getHr() + "홈런, " + hs.getRbi() + "타점, " + hs.getSb() + "도루");
                        }
                    }
                    return dto;
                })
                .collect(Collectors.toList());
    }

    @Transactional
    public void draftPlayer(DraftRequest request) {
        FantasyGame game = fantasyGameRepository.findById(request.getFantasyGameSeq())
                .orElseThrow(() -> new IllegalArgumentException("Invalid Game Seq"));

        boolean isFA = (game.getStatus() == FantasyGame.GameStatus.ONGOING);
        boolean isDrafting = (game.getStatus() == FantasyGame.GameStatus.DRAFTING);

        if (!isDrafting && !isFA) {
            throw new IllegalStateException("Drafting or FA signing is not active (Status: " + game.getStatus() + ")");
        }

        DraftPickExecutor.NextPickInfo nextPick = null;
        if (isDrafting) {
            nextPick = draftPickExecutor.getNextPickInfo(game);
            if (!nextPick.pickerId.equals(request.getMemberId())) {
                throw new IllegalStateException("당신의 차례가 아닙니다: " + nextPick.pickerId);
            }
        }

        boolean isPicked = draftPickRepository.existsByFantasyGameSeqAndFantasyPlayerSeq(
                request.getFantasyGameSeq(), request.getFantasyPlayerSeq());
        if (isPicked) {
            throw new IllegalStateException("이미 뽑힌 선수입니다");
        }

        FantasyPlayer targetPlayer = fantasyPlayerRepository.findById(request.getFantasyPlayerSeq())
                .orElseThrow(() -> new IllegalArgumentException("Invalid Player Seq"));

        List<DraftPick> userPicks = draftPickRepository.findByFantasyGameSeqAndMemberId(
                request.getFantasyGameSeq(), request.getMemberId());
        Set<Long> pickedSeqs = userPicks.stream().map(DraftPick::getFantasyPlayerSeq).collect(Collectors.toSet());
        List<FantasyPlayer> currentTeam = fantasyPlayerRepository.findAllById(pickedSeqs);

        if (isFA) {
            int limit = 21;
            if (userPicks.size() >= limit) {
                throw new IllegalStateException("Roster full (Max " + limit + ")");
            }
        }

        FantasyParticipant participant = fantasyParticipantRepository.findByFantasyGameSeqAndMemberId(
                request.getFantasyGameSeq(), request.getMemberId()).orElse(null);

        if (game.getSalaryCap() != null && game.getSalaryCap() > 0) {
            List<FantasyPlayer> hypotheticalTeam = new java.util.ArrayList<>(currentTeam);
            hypotheticalTeam.add(targetPlayer);
            int projectedCost = com.sayai.record.fantasy.util.SalaryCapCalculator
                    .calculateTeamCost(game, participant, hypotheticalTeam).getTotalCost();
            if (projectedCost > game.getSalaryCap()) {
                throw new IllegalStateException("샐캡 초과: " + projectedCost + " / " + game.getSalaryCap());
            }
        }

        if (isDrafting) {
            draftValidator.validate(game, targetPlayer, currentTeam, participant);
        }

        draftPickExecutor.commitPick(game, request, targetPlayer, isDrafting, nextPick);
    }

    @Transactional(readOnly = true)
    public com.sayai.record.fantasy.dto.MyPicksResponseDto getPickedPlayers(Long gameSeq, Long playerId) {
        List<DraftPick> picks = draftPickRepository.findByFantasyGameSeqAndMemberId(gameSeq, playerId).stream()
                .filter(pick -> pick.getPickStatus() != DraftPick.PickStatus.WAIVER_REQ)
                .collect(Collectors.toList());

        Map<Long, DraftPick> pickMap = picks.stream()
                .collect(Collectors.toMap(DraftPick::getFantasyPlayerSeq, Function.identity()));

        Set<Long> pickedSeqs = pickMap.keySet();
        List<FantasyPlayer> players = fantasyPlayerRepository.findAllById(pickedSeqs);

        FantasyGame game = fantasyGameRepository.findById(gameSeq)
                .orElseThrow(() -> new IllegalArgumentException("Invalid game"));
        FantasyParticipant participant = fantasyParticipantRepository.findByFantasyGameSeqAndMemberId(gameSeq, playerId)
                .orElse(null);

        com.sayai.record.fantasy.util.SalaryCapResult capResult = null;
        if (participant != null) {
            capResult = com.sayai.record.fantasy.util.SalaryCapCalculator.calculateTeamCost(game, participant, players);
        }

        final com.sayai.record.fantasy.util.SalaryCapResult finalCapResult = capResult;

        List<FantasyPlayerDto> dtoList = players.stream()
                .map(p -> {
                    FantasyPlayerDto dto = FantasyPlayerDto.from(p);
                    DraftPick pick = pickMap.get(p.getSeq());
                    if (pick != null) {
                        dto.setAssignedPosition(pick.getAssignedPosition());
                        dto.setOwnerId(pick.getMemberId());
                        dto.setPickStatus(pick.getPickStatus() != null ? pick.getPickStatus().name() : "NORMAL");
                    }
                    if (finalCapResult != null && p.getSeq().equals(finalCapResult.getDiscountedPlayerSeq())) {
                        dto.setDiscounted(true);
                        dto.setCost(finalCapResult.getDiscountedCost());
                    }
                    return dto;
                })
                .sorted(Comparator.comparingInt(p -> {
                    DraftPick pick = pickMap.get(p.getSeq());
                    return pick != null ? pick.getPickNumber() : 0;
                }))
                .collect(Collectors.toList());

        int currentCost = (capResult != null) ? capResult.getTotalCost() :
                players.stream().mapToInt(p -> p.getCost() == null ? 0 : p.getCost()).sum();

        return new com.sayai.record.fantasy.dto.MyPicksResponseDto(dtoList, currentCost);
    }

    @Transactional
    public void updateRoster(Long gameSeq, Long playerId, RosterUpdateDto updateDto) {
        FantasyGame game = fantasyGameRepository.findById(gameSeq)
                .orElseThrow(() -> new IllegalArgumentException("Invalid Game Seq"));

        if (game.getStatus() == FantasyGame.GameStatus.FINISHED) {
            throw new IllegalStateException("Cannot update roster when FINISHED.");
        }

        List<DraftPick> myPicks = draftPickRepository.findByFantasyGameSeqAndMemberId(gameSeq, playerId);
        Map<Long, DraftPick> pickMap = myPicks.stream()
                .collect(Collectors.toMap(DraftPick::getFantasyPlayerSeq, Function.identity()));

        if (game.getSalaryCap() != null && game.getSalaryCap() > 0) {
            Set<Long> pickedSeqs = myPicks.stream()
                    .map(DraftPick::getFantasyPlayerSeq)
                    .collect(Collectors.toSet());
            List<FantasyPlayer> currentTeam = fantasyPlayerRepository.findAllById(pickedSeqs);
            FantasyParticipant participant = fantasyParticipantRepository.findByFantasyGameSeqAndMemberId(gameSeq, playerId).orElse(null);

            int currentCost = com.sayai.record.fantasy.util.SalaryCapCalculator.calculateTeamCost(game, participant, currentTeam).getTotalCost();
            if (currentCost > game.getSalaryCap()) {
                throw new IllegalStateException("샐러리캡을 초과하여 저장할 수 없습니다. (현재: " + currentCost + " / 제한: " + game.getSalaryCap() + ")");
            }
        }

        Set<Long> rosterSeqs = myPicks.stream()
                .map(DraftPick::getFantasyPlayerSeq)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        List<FantasyPlayer> rosterPlayers = fantasyPlayerRepository.findAllById(rosterSeqs);
        long type1Count = rosterPlayers.stream()
                .filter(p -> p.getForeignerType() == FantasyPlayer.ForeignerType.TYPE_1)
                .count();
        long type2Count = rosterPlayers.stream()
                .filter(p -> p.getForeignerType() == FantasyPlayer.ForeignerType.TYPE_2)
                .count();
        if (type1Count > 3) {
            throw new IllegalStateException("외국인 용병 제한 3명을 넘게 선발할 수 없습니다.");
        }
        if (type2Count > 1) {
            throw new IllegalStateException("아시아 쿼터 제한 1명을 넘게 선발할 수 없습니다.");
        }

        if (Boolean.TRUE.equals(game.getUseTeamRestriction())) {
            rule1Validator.validateFinalTeamCoverage(rosterPlayers);
        }

        if (updateDto.getEntries() != null) {
            Map<String, Integer> positionCounts = new java.util.HashMap<>();

            java.util.function.BiConsumer<String, String> checkLimit = (pos, source) -> {
                if ("BENCH".equals(pos)) return;
                int count = positionCounts.getOrDefault(pos, 0) + 1;
                positionCounts.put(pos, count);
                int limit;
                if ("SP".equals(pos) || "RP".equals(pos)) limit = 4;
                else limit = 1;
                if (count > limit) {
                    throw new IllegalArgumentException("Position limit exceeded for " + pos + " (Max " + limit + ")");
                }
            };

            Set<Long> updatingSeqs = updateDto.getEntries().stream()
                    .map(RosterUpdateDto.RosterEntry::getFantasyPlayerSeq)
                    .collect(Collectors.toSet());
            for (DraftPick pick : myPicks) {
                if (!updatingSeqs.contains(pick.getFantasyPlayerSeq()) && pick.getAssignedPosition() != null) {
                    checkLimit.accept(pick.getAssignedPosition(), "Existing");
                }
            }

            for (RosterUpdateDto.RosterEntry entry : updateDto.getEntries()) {
                if (entry.getAssignedPosition() != null) {
                    checkLimit.accept(entry.getAssignedPosition(), "New");
                }
            }

            for (RosterUpdateDto.RosterEntry entry : updateDto.getEntries()) {
                DraftPick pick = pickMap.get(entry.getFantasyPlayerSeq());
                if (pick != null) {
                    String pos = entry.getAssignedPosition();
                    if (pos != null && pos.startsWith("BENCH")) {
                        pos = "BENCH";
                    }
                    pick.setAssignedPosition(pos);
                }
            }
        }
        draftPickRepository.saveAll(myPicks);
    }
}
