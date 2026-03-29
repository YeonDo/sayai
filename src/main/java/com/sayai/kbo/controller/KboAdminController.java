package com.sayai.kbo.controller;

import com.sayai.kbo.dto.KboGameUploadRequest;
import com.sayai.kbo.service.KboAdminService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/apis/v1/admin/kbo")
@RequiredArgsConstructor
public class KboAdminController {

    private final KboAdminService kboAdminService;

    @PostMapping("/game/upload")
    public ResponseEntity<String> uploadGameRecords(@ModelAttribute KboGameUploadRequest request) {
        try {
            kboAdminService.uploadGame(request);
            return ResponseEntity.ok("Game records uploaded successfully.");
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.badRequest().body("Failed to process game records: " + e.getMessage());
        }
    }
}
