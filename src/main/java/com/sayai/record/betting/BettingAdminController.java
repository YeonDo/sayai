package com.sayai.record.betting;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/apis/v1/admin/bets")
@RequiredArgsConstructor
public class BettingAdminController {

    private final BettingService bettingService;

    /** 베팅 항목 생성. deadline은 ISO-8601(오프셋 포함, 예: 2026-10-20T21:00:00+09:00) → UTC 저장 */
    @PostMapping
    public ResponseEntity<BettingService.BtGameDto> create(@RequestBody BettingService.CreateRequest request) {
        return ResponseEntity.ok(bettingService.create(request));
    }

    /** 전체 항목 (OPEN/CLOSED/CANCELED, 최신순) */
    @GetMapping
    public ResponseEntity<List<BettingService.BtGameDto>> all() {
        return ResponseEntity.ok(bettingService.allGames());
    }

    /** 항목별 베팅 내역 */
    @GetMapping("/{gameId}/logs")
    public ResponseEntity<List<BettingService.BtLogDto>> logs(@PathVariable(name = "gameId") Long gameId) {
        return ResponseEntity.ok(bettingService.logsOf(gameId));
    }

    /** 무효 처리: 전액 환불 후 CANCELED. OPEN 상태만 가능 */
    @PostMapping("/{gameId}/cancel")
    public ResponseEntity<BettingService.BtGameDto> cancel(@PathVariable(name = "gameId") Long gameId) {
        return ResponseEntity.ok(bettingService.cancel(gameId));
    }

    /** 정답 입력 → 종료 → 적중자 배당 지급(적중자 없으면 전액 환불) */
    @PostMapping("/{gameId}/close")
    public ResponseEntity<BettingService.BtGameDto> close(@PathVariable(name = "gameId") Long gameId,
                                                          @RequestBody Map<String, String> body) {
        return ResponseEntity.ok(bettingService.close(gameId, body.get("answer")));
    }
}
