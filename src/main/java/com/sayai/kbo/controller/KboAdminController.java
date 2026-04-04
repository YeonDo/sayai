package com.sayai.kbo.controller;

import com.sayai.kbo.dto.KboGameUploadRequest;
import com.sayai.kbo.dto.KboGameUploadResponse;
import com.sayai.kbo.service.KboAdminService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/apis/v1/admin/kbo")
@RequiredArgsConstructor
public class KboAdminController {

    private final KboAdminService kboAdminService;

    @GetMapping("/game/{gameIdx}")
    public ResponseEntity<?> getGameDetails(@PathVariable("gameIdx") Long gameIdx) {
        try {
            return ResponseEntity.ok(kboAdminService.getGameDetails(gameIdx));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PostMapping("/game/upload")
    public ResponseEntity<?> uploadGameRecords(@ModelAttribute KboGameUploadRequest request) {
        try {
            KboGameUploadResponse response = kboAdminService.uploadGame(request);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.badRequest().body("Failed to process game records: " + e.getMessage());
        }
    }
}
