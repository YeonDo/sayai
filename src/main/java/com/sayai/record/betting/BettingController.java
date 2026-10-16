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

    /** 현재 열린 베팅 항목 (객관식은 문항별 풀·배당률 포함) */
    @GetMapping("/games")
    public ResponseEntity<List<BettingService.BtGameDto>> openGames() {
        return ResponseEntity.ok(bettingService.openGames());
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
