package com.sayai.record.betting;

import com.sayai.record.auth.jwt.CustomUserDetails;
import com.sayai.record.point.PointService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/apis/v1/bet")
@RequiredArgsConstructor
public class BettingController {

    private final BettingService bettingService;
    private final PointService pointService;

    public record BetRequest(String answer, Long amount) {}

    /** 베팅 항목 목록. status 기본 OPEN. CLOSED면 정답·최종 배당(finalOdds) 포함 */
    @GetMapping("/games")
    public ResponseEntity<List<BettingService.BtGameDto>> games(
            @RequestParam(name = "status", defaultValue = "OPEN") BtGame.Status status) {
        return ResponseEntity.ok(bettingService.games(status));
    }

    /** 내 베팅 내역 (최신순) */
    @GetMapping("/my")
    public ResponseEntity<List<BettingService.MyBetDto>> myBets(@AuthenticationPrincipal CustomUserDetails userDetails) {
        if (userDetails == null) {
            return ResponseEntity.status(401).build();
        }
        return ResponseEntity.ok(bettingService.myBets(userDetails.getMemberId()));
    }

    /** 베팅. answer: 객관식=문항 index, 주관식=숫자. 마감 후 409, 잔액 부족 400. */
    @PostMapping("/games/{gameId}")
    public ResponseEntity<Map<String, Object>> bet(@PathVariable(name = "gameId") Long gameId,
                                                   @RequestBody BetRequest request,
                                                   @AuthenticationPrincipal CustomUserDetails userDetails) {
        if (userDetails == null) {
            return ResponseEntity.status(401).build();
        }
        BtLog log = bettingService.bet(userDetails.getMemberId(), gameId, request.answer(), request.amount());
        return ResponseEntity.ok(Map.of(
                "logId", log.getId(),
                "answer", log.getAnswer(),
                "amount", log.getAmount(),
                "balance", pointService.getBalance(userDetails.getMemberId())));
    }
}
