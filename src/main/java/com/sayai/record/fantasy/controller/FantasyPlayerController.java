package com.sayai.record.fantasy.controller;

import com.sayai.record.fantasy.dto.FantasyPlayerDto;
import com.sayai.record.fantasy.service.FantasyPlayerService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/apis/v1/fantasy/players")
@RequiredArgsConstructor
public class FantasyPlayerController {

    private final FantasyPlayerService fantasyPlayerService;

    @PutMapping("/{seq}")
    public ResponseEntity<String> updatePlayer(@PathVariable Long seq, @RequestBody FantasyPlayerDto dto) {
        try {
            fantasyPlayerService.updatePlayer(seq, dto);
            return ResponseEntity.ok("Player updated successfully");
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Update failed: " + e.getMessage());
        }
    }

    @PostMapping("/import")
    public ResponseEntity<String> importPlayers(@RequestParam("file") MultipartFile file) {
        if (file.isEmpty()) {
            return ResponseEntity.badRequest().body("File is empty");
        }

        try {
            fantasyPlayerService.importPlayers(file);
            return ResponseEntity.ok("Import successful");
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Import failed: " + e.getMessage());
        }
    }
}
