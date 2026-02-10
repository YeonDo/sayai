package com.sayai.record.fantasy.controller;

import com.sayai.record.auth.entity.Member;
import com.sayai.record.auth.repository.MemberRepository;
import com.sayai.record.fantasy.dto.RoasterLogDto;
import com.sayai.record.fantasy.entity.FantasyParticipant;
import com.sayai.record.fantasy.entity.FantasyPlayer;
import com.sayai.record.fantasy.entity.RoasterLog;
import com.sayai.record.fantasy.repository.FantasyParticipantRepository;
import com.sayai.record.fantasy.repository.FantasyPlayerRepository;
import com.sayai.record.fantasy.repository.RoasterLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/apis/v1/fantasy")
@RequiredArgsConstructor
public class FantasyLogController {

    private final RoasterLogRepository roasterLogRepository;
    private final FantasyPlayerRepository fantasyPlayerRepository;
    private final FantasyParticipantRepository fantasyParticipantRepository;
    private final MemberRepository memberRepository;

    @GetMapping("/games/{gameSeq}/logs")
    public ResponseEntity<List<RoasterLogDto>> getLogs(@PathVariable(name = "gameSeq") Long gameSeq) {
        // Sort by timestamp ASC as requested ("점점 로그가 밑으로 쌓이는거야")
        List<RoasterLog> logs = roasterLogRepository.findAll(Sort.by(Sort.Direction.ASC, "timestamp")).stream()
                .filter(l -> l.getFantasyGameSeq().equals(gameSeq))
                .collect(Collectors.toList());

        // Batch fetch players
        Set<Long> playerSeqs = logs.stream()
                .map(RoasterLog::getFantasyPlayerSeq)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        Map<Long, FantasyPlayer> playerMap = fantasyPlayerRepository.findAllById(playerSeqs).stream()
                .collect(Collectors.toMap(FantasyPlayer::getSeq, java.util.function.Function.identity()));

        // Batch fetch participants
        // Note: logs store participantId (which is player_id in Member/Participant table usually)
        // But participant info (team name) is in FantasyParticipant.
        // FantasyParticipant links gameSeq + playerId -> teamName.
        // However, if user left, might be tricky. But usually they persist.
        // Let's fetch all participants for this game.
        Map<Long, String> participantTeamNames = fantasyParticipantRepository.findByFantasyGameSeq(gameSeq).stream()
                .collect(Collectors.toMap(FantasyParticipant::getPlayerId,
                        p -> p.getTeamName() != null ? p.getTeamName() : "Unknown Team"));

        // If team name missing from Participant table (e.g. log from before join? Unlikely), fallback to User Name.
        // Or just show ID. Let's try to fetch Member names too for fallback.
        Set<Long> participantIds = logs.stream().map(RoasterLog::getParticipantId).filter(Objects::nonNull).collect(Collectors.toSet());
        Map<Long, String> memberNames = memberRepository.findAllById(participantIds).stream()
                .collect(Collectors.toMap(Member::getPlayerId, Member::getName));

        List<RoasterLogDto> dtos = logs.stream().map(log -> {
            FantasyPlayer p = playerMap.get(log.getFantasyPlayerSeq());
            String pName = "", pTeam = "", pPos = "";

            boolean showDetails = log.getActionType() == RoasterLog.LogActionType.DRAFT_PICK
                               || log.getActionType() == RoasterLog.LogActionType.FA_ADD
                               || log.getActionType().name().startsWith("WAIVER_");

            if (p != null) {
                // For Waiver/Trade/Draft/FA, show name
                // Actually previous request said: "Player (드래프트, FA인 경우 ft_players 의 name, 웨이버, 트레이드인 경우 빈칸)"
                // NEW Request: "draft-log 에서 WAVIER_RELEASE, WAIVER_FA, WAIVER_CLAIM 인 경우에도 해당 선수의 player, team, position 항목이 표시되게 해줘"
                // Trade still empty? "트레이드" is not mentioned in new request, so keep empty for Trade.
                if (showDetails) {
                    pName = p.getName();
                    pTeam = p.getTeam();
                    pPos = p.getPosition();
                }
            } else if (showDetails) {
                pName = "Unknown Player";
            }

            // Participant Name
            // "Participants (드래프트, FA, 웨이버, 트레이드를 신청한 참가자. ft_participants 의 team_name)"
            String partName = participantTeamNames.getOrDefault(log.getParticipantId(),
                    memberNames.getOrDefault(log.getParticipantId(), String.valueOf(log.getParticipantId())));

            return RoasterLogDto.builder()
                    .seq(log.getSeq())
                    .playerName(pName)
                    .playerTeam(pTeam)
                    .playerPosition(pPos)
                    .participantName(partName)
                    .actionType(log.getActionType().name())
                    .details(log.getDetails())
                    .timestamp(log.getTimestamp())
                    .build();
        }).collect(Collectors.toList());

        return ResponseEntity.ok(dtos);
    }
}
