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

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
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
                            Double finalOdds, List<ChoiceDto> choices) {}

    public record MyBetDto(Long logId, Long gameId, String title, BtGame.Type type, BtGame.Status status,
                           Instant deadline, String correctAnswer, String answer, String answerLabel,
                           long amount, Long payout, Instant createdAt) {}

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

    /** NUMBER 당첨 구간: 오차순 누적 베팅액 기준 상위 비율. 이 지점에서 가중치가 0이 된다 */
    static final double NUMBER_CUTOFF = 0.5;
    /** NUMBER 가중치 감소 지수. 1은 선형, 클수록 정답 근처에 집중 */
    static final int NUMBER_SHARPNESS = 2;

    /**
     * 정산. 반환: logId → 지급 포인트 (미포함 = 0). 지급 합계 = 전체 풀 (수수료 0).
     * CHOICE: 지급액 = 베팅액 × (전체 풀 / 적중 풀), 소수점 버림. 적중자가 없으면 전액 환불.
     * NUMBER: {@link #numberPayouts} 참고.
     */
    static Map<Long, Long> computePayouts(BtGame.Type type, String answer, List<BtLog> logs) {
        Map<Long, Long> result = new HashMap<>();
        if (logs.isEmpty()) return result;
        if (type == BtGame.Type.NUMBER) return numberPayouts(new BigDecimal(answer), logs);

        List<BtLog> winners = logs.stream().filter(l -> l.getAnswer().equals(answer)).toList();
        if (winners.isEmpty()) {
            logs.forEach(l -> result.put(l.getId(), l.getAmount()));
            return result;
        }
        long total = logs.stream().mapToLong(BtLog::getAmount).sum();
        long winPool = winners.stream().mapToLong(BtLog::getAmount).sum();
        winners.forEach(l -> result.put(l.getId(), l.getAmount() * total / winPool));
        return result;
    }

    /**
     * NUMBER 정산 (베팅 단위).
     * 1. 오차 |값 - 정답| 오름차순 정렬. 같은 오차는 한 그룹.
     * 2. 그룹 위치 p = (앞 그룹 누적 베팅액 + 그룹 베팅액/2) / 풀.
     * 3. 가중치 w = max(0, 1 - p/CUTOFF)^SHARPNESS, 점수 = 베팅액 × w.
     * 4. 지급액 = floor(풀 × 점수 / 점수합). 자투리는 점수 최대 베팅(동률이면 오차 작은 것, 그다음 먼저 건 것)에.
     * 점수합이 0이면(전원 같은 오차) 전액 환불.
     */
    private static Map<Long, Long> numberPayouts(BigDecimal target, List<BtLog> logs) {
        Map<Long, BigDecimal> err = new HashMap<>();
        logs.forEach(l -> err.put(l.getId(), new BigDecimal(l.getAnswer()).subtract(target).abs()));
        // 오차는 BigDecimal로 비교해 부동소수점 동률 오판을 막는다. id는 자투리 수령자를 입력 순서와 무관하게 고정
        List<BtLog> rows = logs.stream()
                .sorted(Comparator.comparing((BtLog l) -> err.get(l.getId())).thenComparing(BtLog::getId))
                .toList();
        long pool = rows.stream().mapToLong(BtLog::getAmount).sum();

        Map<Long, Double> scores = new HashMap<>();
        long cum = 0;
        for (int i = 0; i < rows.size(); ) {
            BigDecimal e = err.get(rows.get(i).getId());
            int j = i;
            long group = 0;
            while (j < rows.size() && err.get(rows.get(j).getId()).compareTo(e) == 0) group += rows.get(j++).getAmount();
            double p = (cum + group / 2.0) / pool;
            double w = Math.pow(Math.max(0, 1 - p / NUMBER_CUTOFF), NUMBER_SHARPNESS);
            for (int x = i; x < j; x++) scores.put(rows.get(x).getId(), rows.get(x).getAmount() * w);
            cum += group;
            i = j;
        }

        Map<Long, Long> result = new HashMap<>();
        double totalScore = scores.values().stream().mapToDouble(Double::doubleValue).sum();
        if (totalScore == 0) {
            rows.forEach(l -> result.put(l.getId(), l.getAmount()));
            return result;
        }
        long paid = 0;
        BtLog top = rows.get(0);
        for (BtLog l : rows) {
            double s = scores.get(l.getId());
            if (s == 0) continue;
            long payout = (long) Math.floor(pool * s / totalScore);
            result.put(l.getId(), payout);
            paid += payout;
            if (s > scores.get(top.getId())) top = l;
        }
        result.merge(top.getId(), pool - paid, Long::sum);
        return result;
    }

    // --- 멤버 ---

    /** 상태별 항목. OPEN은 마감 임박순, 그 외는 최신순 */
    @Transactional(readOnly = true)
    public List<BtGameDto> games(BtGame.Status status) {
        List<BtGame> games = status == BtGame.Status.OPEN
                ? btGameRepository.findByStatusOrderByDeadlineAsc(status)
                : btGameRepository.findByStatusOrderByIdDesc(status);
        Map<Long, List<BtLog>> logsByGame = btLogRepository
                .findByBtGameIdIn(games.stream().map(BtGame::getId).toList())
                .stream().collect(Collectors.groupingBy(BtLog::getBtGameId));
        return games.stream().map(g -> toDto(g, logsByGame.getOrDefault(g.getId(), List.of()))).toList();
    }

    /** 내 베팅 내역 (최신순). 항목 제목·상태·정답 포함 */
    @Transactional(readOnly = true)
    public List<MyBetDto> myBets(Long memberId) {
        List<BtLog> logs = btLogRepository.findByMemberIdOrderByIdDesc(memberId);
        Map<Long, BtGame> games = btGameRepository.findAllById(logs.stream().map(BtLog::getBtGameId).distinct().toList())
                .stream().collect(Collectors.toMap(BtGame::getId, g -> g));
        Map<Long, List<String>> labels = new HashMap<>();
        return logs.stream().map(l -> {
            BtGame g = games.get(l.getBtGameId());
            String label = l.getAnswer();
            if (g.getType() == BtGame.Type.CHOICE) {
                List<String> ls = labels.computeIfAbsent(g.getId(), id -> choicesOf(g));
                int idx = Integer.parseInt(l.getAnswer());
                if (idx < ls.size()) label = ls.get(idx);
            }
            return new MyBetDto(l.getId(), g.getId(), g.getTitle(), g.getType(), g.getStatus(), g.getDeadline(),
                    g.getAnswer(), l.getAnswer(), label, l.getAmount(), l.getPayout(), l.getCreatedAt());
        }).toList();
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
            double v = Double.parseDouble(a);
            // NaN·Infinity는 파싱은 되지만 정산 시 오차 계산이 깨진다
            if (!Double.isFinite(v)) throw new IllegalArgumentException("answer 형식이 올바르지 않습니다: " + a);
            return String.valueOf(v);
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
        // 종료 후 최종 배당 = 전체 풀 / 적중 풀 (환불이면 1.0). 종료 전·취소·베팅 없음이면 null
        // NUMBER는 베팅마다 배당이 달라 단일 배당이 없으므로 null
        Double finalOdds = null;
        if (g.getStatus() == BtGame.Status.CLOSED && g.getType() == BtGame.Type.CHOICE) {
            long winPool = logs.stream().filter(l -> l.getPayout() != null && l.getPayout() > 0).mapToLong(BtLog::getAmount).sum();
            if (winPool > 0) finalOdds = Math.round((double) total / winPool * 100) / 100.0;
        }
        return new BtGameDto(g.getId(), g.getTitle(), g.getContent(), g.getType(), g.getDeadline(),
                g.getStatus(), g.getAnswer(), g.isBettable(Instant.now()), total, finalOdds, choices);
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
