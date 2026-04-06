package com.sayai.record.firebase;

import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Collections;

@RestController
@RequestMapping("/apis/v1/fcm")
@RequiredArgsConstructor
public class FcmController {

    private final FcmService fcmService;

    @PostMapping("/subscribe")
    public ResponseEntity<String> subscribeToken(@RequestBody SubscribeRequest request) {
        if (request.getToken() != null && !request.getToken().isEmpty() && request.getTopic() != null) {
            fcmService.subscribeToTopic(Collections.singletonList(request.getToken()), request.getTopic());
        }
        return ResponseEntity.ok("Subscribed");
    }

    @Data
    public static class SubscribeRequest {
        private String token;
        private String topic;
    }
}
