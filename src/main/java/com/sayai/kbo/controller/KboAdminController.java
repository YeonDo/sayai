package com.sayai.kbo.controller;

import com.sayai.kbo.dto.KboGameUploadRequest;
import com.sayai.kbo.dto.KboGameUploadResponse;
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
