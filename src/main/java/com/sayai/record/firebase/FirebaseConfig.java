package com.sayai.record.firebase;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;

import jakarta.annotation.PostConstruct;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.IOException;

@Slf4j
@Configuration
public class FirebaseConfig {

    @Value("${firebase.key-path:}")
    private String serviceAccountPath;

    @PostConstruct
    public void init() {
        try {
            FirebaseOptions options = null;
            if (serviceAccountPath != null && !serviceAccountPath.isEmpty()) {
                InputStream serviceAccount = null;
                try {
                    serviceAccount = new ClassPathResource(serviceAccountPath).getInputStream();
                } catch (IOException e) {
                    log.warn("Could not find Firebase key on classpath: {}. Trying file system.", serviceAccountPath);
                    try {
                        serviceAccount = new FileInputStream(serviceAccountPath);
                    } catch (IOException ex) {
                        log.error("Failed to load Firebase key from file system.", ex);
                    }
                }

                if (serviceAccount != null) {
                    options = FirebaseOptions.builder()
                            .setCredentials(GoogleCredentials.fromStream(serviceAccount))
                            .build();
                }
            }

            if (options == null) {
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
