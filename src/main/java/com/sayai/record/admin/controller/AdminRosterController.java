package com.sayai.record.admin.controller;

import com.sayai.record.fantasy.entity.RosterTransaction;
import com.sayai.record.fantasy.service.FantasyRosterService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/apis/v1/admin/fantasy")
@RequiredArgsConstructor
public class AdminRosterController {

    private final FantasyRosterService fantasyRosterService;

    @GetMapping("/transactions")
    public ResponseEntity<List<RosterTransaction>> getTransactions(@RequestParam(name = "gameSeq") Long gameSeq,
                                                                    @RequestParam(name = "status", required = false) String status) {
        return ResponseEntity.ok(fantasyRosterService.getTransactions(gameSeq, status));
    }

    @PostMapping("/transactions/{seq}/process")
    public ResponseEntity<String> processTransaction(@PathVariable(name = "seq") Long transactionSeq,
                                                     @RequestBody ProcessTransactionRequest request) {
        try {
            fantasyRosterService.processTransaction(transactionSeq, request.getDecision(), request.getTargetParticipantId());
            return ResponseEntity.ok("Transaction Processed");
        } catch (IllegalArgumentException | IllegalStateException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @Data
    public static class ProcessTransactionRequest {
        private String decision;
        private Long targetParticipantId; // Optional, for Waiver Claim
    }
}
