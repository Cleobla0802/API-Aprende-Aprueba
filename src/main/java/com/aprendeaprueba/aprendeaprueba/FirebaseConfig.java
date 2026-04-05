package com.aprendeaprueba.aprendeaprueba;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import jakarta.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;

import java.io.IOException;

@Configuration
public class FirebaseConfig {

	@Value("${firebase.database.url}")
    private String databaseUrl;

    @PostConstruct
    public void initialize() {
        try {
            FirebaseOptions options = FirebaseOptions.builder()
                .setCredentials(GoogleCredentials.fromStream(
                    new ClassPathResource("serviceAccountKey.json").getInputStream()))
                .setDatabaseUrl(databaseUrl)
                .build();

            if (FirebaseApp.getApps().isEmpty()) {
                FirebaseApp.initializeApp(options);
                System.out.println("Firebase inicializado con éxito en: " + databaseUrl);
            }
        } catch (IOException e) {
            System.err.println("Error al inicializar Firebase: " + e.getMessage());
        }
    }
}