package com.sayai.record.betting;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sayai.record.auth.entity.Member;
import com.sayai.record.auth.repository.MemberRepository;
import com.sayai.record.point.PointService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class BettingService {

    private final BtGameRepository btGameRepository;
    private final BtLogRepository btLogRepository;
    private final PointService pointService;
    private final MemberRepository memberRepository;
    private final ObjectMapper objectMapper;

    public record ChoiceDto(int index, String label, long pool, Double odds) {}

    public record BtGameDto(Long id, String title, String content, BtGame.Type type, Instant deadline,
                            BtGame.Status status, String answer, boolean bettable, long totalPool,
                            List<ChoiceDto> choices) {}

    public record BtLogDto(Long id, Long memberId, String memberName, String answer, long amount, Long payout, Instant createdAt) {}

    public record CreateRequest(String title, String content, BtGame.Type type, List<String> choices, Instant deadline) {}

    // --- 어드민 ---

    @Transactional
    public BtGameDto create(CreateRequest r) {
        if (r.title() == null || r.title().isBlank()) throw new IllegalArgumentException("title은 필수입니다.");
        if (r.type() == null) throw new IllegalArgumentException("type은 CHOICE 또는 NUMBER여야 합니다.");
        if (r.deadline() == null) throw new IllegalArgumentException("deadline은 필수입니다.");
        if (r.deadline().isBefore(Instant.now())) throw new IllegalArgumentException("deadline이 이미 지났습니다.");

        String choicesJson = null;
        if (r.type() == BtGame.Type.CHOICE) {
            if (r.choices() == null || r.choices().size() < 2 || r.choices().stream().anyMatch(c -> c == null || c.isBlank())) {
                throw new IllegalArgumentException("객관식은 문항이 2개 이상 필요합니다.");
            }
            choicesJson = toJson(r.choices());
        }

        BtGame game = btGameRepository.save(BtGame.builder()
                .title(r.title()).content(r.content()).type(r.type())
                .choices(choicesJson).deadline(r.deadline()).build());
        return toDto(game, List.of());
    }

    /** 전체 항목 (상태 무관, 최신순) */
    @Transactional(readOnly = true)
    public List<BtGameDto> allGames() {
        List<BtGame> games = btGameRepository.findAllByOrderByIdDesc();
        Map<Long, List<BtLog>> logsByGame = btLogRepository
                .findByBtGameIdIn(games.stream().map(BtGame::getId).toList())
                .stream().collect(Collectors.groupingBy(BtLog::getBtGameId));
        return games.stream().map(g -> toDto(g, logsByGame.getOrDefault(g.getId(), List.of()))).toList();
    }

    /** 항목별 베팅 내역 (멤버명 포함) */
    @Transactional(readOnly = true)
    public List<BtLogDto> logsOf(Long gameId) {
        List<BtLog> logs = btLogRepository.findByBtGameId(gameId);
        Map<Long, String> names = memberRepository.findAllById(logs.stream().map(BtLog::getMemberId).distinct().toList())
                .stream().collect(Collectors.toMap(Member::getMemberId, Member::getName));
        return logs.stream().map(l -> new BtLogDto(l.getId(), l.getMemberId(), names.getOrDefault(l.getMemberId(), "Unknown"),
                l.getAnswer(), l.getAmount(), l.getPayout(), l.getCreatedAt())).toList();
    }

    /** 무효 처리: 전액 환불 후 CANCELED */
    @Transactional
    public BtGameDto cancel(Long gameId) {
        BtGame game = btGameRepository.findForUpdate(gameId)
                .orElseThrow(() -> new IllegalArgumentException("베팅 항목을 찾을 수 없습니다: " + gameId));
        game.cancel(Instant.now());
        List<BtLog> logs = btLogRepository.findByBtGameId(gameId);
        for (BtLog log : logs) {
            log.setPayout(log.getAmount());
            pointService.adjust(log.getMemberId(), log.getAmount());
        }
        return toDto(game, logs);
    }

    @Transactional
    public BtGameDto close(Long gameId, String answer) {
        BtGame game = btGameRepository.findForUpdate(gameId)
                .orElseThrow(() -> new IllegalArgumentException("베팅 항목을 찾을 수 없습니다: " + gameId));
        String normalized = normalizeAnswer(game, answer);
        game.close(normalized, Instant.now());

        List<BtLog> logs = btLogRepository.findByBtGameId(gameId);
        Map<Long, Long> payouts = computePayouts(game.getType(), normalized, logs);
        for (BtLog log : logs) {
            long payout = payouts.getOrDefault(log.getId(), 0L);
            log.setPayout(payout);
            if (payout > 0) pointService.adjust(log.getMemberId(), payout);
        }
        return toDto(game, logs);
    }

    /**
     * 정산. 배당 = 베팅액 × (전체 풀 / 적중 풀), 소수점 버림.
     * CHOICE: 정답 index와 같은 베팅이 적중. NUMBER: 정답에 가장 근접한 값(동률 전원)이 적중.
     * 적중자가 없으면 전액 환불. 반환: logId → 지급 포인트 (미포함 = 0).
     */
    static Map<Long, Long> computePayouts(BtGame.Type type, String answer, List<BtLog> logs) {
        Map<Long, Long> result = new HashMap<>();
        if (logs.isEmpty()) return result;

        List<BtLog> winners;
        if (type == BtGame.Type.CHOICE) {
            winners = logs.stream().filter(l -> l.getAnswer().equals(answer)).toList();
        } else {
            double target = Double.parseDouble(answer);
            double min = logs.stream().mapToDouble(l -> Math.abs(Double.parseDouble(l.getAnswer()) - target)).min().orElse(0);
            winners = logs.stream().filter(l -> Math.abs(Double.parseDouble(l.getAnswer()) - target) == min).toList();
        }

        if (winners.isEmpty()) {
            logs.forEach(l -> result.put(l.getId(), l.getAmount()));
            return result;
        }
        long total = logs.stream().mapToLong(BtLog::getAmount).sum();
        long winPool = winners.stream().mapToLong(BtLog::getAmount).sum();
        winners.forEach(l -> result.put(l.getId(), l.getAmount() * total / winPool));
        return result;
    }

    // --- 멤버 ---

    @Transactional(readOnly = true)
    public List<BtGameDto> openGames() {
        List<BtGame> games = btGameRepository.findByStatusOrderByDeadlineAsc(BtGame.Status.OPEN);
        Map<Long, List<BtLog>> logsByGame = btLogRepository
                .findByBtGameIdIn(games.stream().map(BtGame::getId).toList())
                .stream().collect(Collectors.groupingBy(BtLog::getBtGameId));
        return games.stream().map(g -> toDto(g, logsByGame.getOrDefault(g.getId(), List.of()))).toList();
    }

    @Transactional
    public BtLog bet(Long memberId, Long gameId, String answer, Long amount) {
        if (amount == null || amount <= 0) throw new IllegalArgumentException("베팅 포인트는 1 이상이어야 합니다.");
        BtGame game = btGameRepository.findById(gameId)
                .orElseThrow(() -> new IllegalArgumentException("베팅 항목을 찾을 수 없습니다: " + gameId));
        if (!game.isBettable(Instant.now())) throw new IllegalStateException("마감된 베팅입니다.");
        String normalized = normalizeAnswer(game, answer);

        pointService.adjust(memberId, -amount);
        return btLogRepository.save(BtLog.builder()
                .btGameId(gameId).memberId(memberId).answer(normalized).amount(amount).build());
    }

    // --- helpers ---

    /** CHOICE: 문항 index 문자열로, NUMBER: 숫자 문자열로 검증·정규화 */
    private String normalizeAnswer(BtGame game, String answer) {
        if (answer == null || answer.isBlank()) throw new IllegalArgumentException("answer는 필수입니다.");
        String a = answer.trim();
        try {
            if (game.getType() == BtGame.Type.CHOICE) {
                int idx = Integer.parseInt(a);
                int size = choicesOf(game).size();
                if (idx < 0 || idx >= size) throw new IllegalArgumentException("문항 index 범위 밖: " + idx);
                return String.valueOf(idx);
            }
            return String.valueOf(Double.parseDouble(a));
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("answer 형식이 올바르지 않습니다: " + a);
        }
    }

    private BtGameDto toDto(BtGame g, List<BtLog> logs) {
        long total = logs.stream().mapToLong(BtLog::getAmount).sum();
        List<ChoiceDto> choices = null;
        if (g.getType() == BtGame.Type.CHOICE) {
            Map<String, Long> poolByAnswer = logs.stream()
                    .collect(Collectors.groupingBy(BtLog::getAnswer, Collectors.summingLong(BtLog::getAmount)));
            List<String> labels = choicesOf(g);
            choices = new ArrayList<>();
            for (int i = 0; i < labels.size(); i++) {
                long pool = poolByAnswer.getOrDefault(String.valueOf(i), 0L);
                Double odds = pool == 0 ? null : Math.round((double) total / pool * 100) / 100.0;
                choices.add(new ChoiceDto(i, labels.get(i), pool, odds));
            }
        }
        return new BtGameDto(g.getId(), g.getTitle(), g.getContent(), g.getType(), g.getDeadline(),
                g.getStatus(), g.getAnswer(), g.isBettable(Instant.now()), total, choices);
    }

    private List<String> choicesOf(BtGame g) {
        if (g.getChoices() == null) return List.of();
        try {
            return objectMapper.readValue(g.getChoices(), new TypeReference<List<String>>() {});
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("choices JSON 파싱 실패: " + g.getId(), e);
        }
    }

    private String toJson(List<String> choices) {
        try {
            return objectMapper.writeValueAsString(choices);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("choices 직렬화 실패", e);
        }
    }
}
