package com.sayai.record.firebase;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import jakarta.annotation.PostConstruct;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

@Slf4j
@Configuration
public class FirebaseConfig {

    @Value("${firebase.certification:}")
    private String fcmCertification;

    @PostConstruct
    public void init() {
        try {
            FirebaseOptions options;
            if (fcmCertification != null && !fcmCertification.trim().isEmpty()) {
                InputStream serviceAccount = new ByteArrayInputStream(fcmCertification.getBytes(StandardCharsets.UTF_8));
                options = FirebaseOptions.builder()
                        .setCredentials(GoogleCredentials.fromStream(serviceAccount))
                        .build();
            } else {
                log.warn("Firebase certification string is empty. Using application default credentials. If this fails, FCM won't work.");
                try {
                    options = FirebaseOptions.builder()
                            .setCredentials(GoogleCredentials.getApplicationDefault())
                            .build();
                } catch (IOException e) {
                    log.warn("Could not load default credentials. Skipping Firebase initialization for local development.");
                    return;
                }
            }

            if (FirebaseApp.getApps().isEmpty()) {
                FirebaseApp.initializeApp(options);
                log.info("Firebase application initialized");
            }
        } catch (IOException e) {
            log.error("Failed to initialize Firebase", e);
        }
    }
}
