package com.sayai.record.firebase;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import jakarta.annotation.PostConstruct;
import java.io.FileInputStream;
import java.io.IOException;

@Slf4j
@Configuration
public class FirebaseConfig {

    @Value("${firebase.key-path}")
    private String serviceAccountPath;

    @PostConstruct
    public void init() {
        try {
            FirebaseOptions options;
            if (serviceAccountPath != null && !serviceAccountPath.isEmpty()) {
                FileInputStream serviceAccount = new FileInputStream(serviceAccountPath);
                options = FirebaseOptions.builder()
                        .setCredentials(GoogleCredentials.fromStream(serviceAccount))
                        .build();
            } else {
                log.warn("Firebase service account path is empty. Using application default credentials. If this fails, FCM won't work.");
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
