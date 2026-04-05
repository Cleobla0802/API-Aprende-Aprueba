package com.aprendeaprueba.aprendeaprueba;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import jakarta.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

@Configuration
public class FirebaseConfig {

    @Value("${firebase.database.url}")
    private String databaseUrl;

    @PostConstruct
    public void initialize() {
        try {
            // 1. Intentamos leer la variable de entorno que configuraremos en Render
            String firebaseConfigEnv = System.getenv("FIREBASE_CONFIG");
            InputStream serviceAccount;

            if (firebaseConfigEnv != null && !firebaseConfigEnv.isEmpty()) {
                // Si existe la variable (estamos en Render), usamos el texto del JSON
                serviceAccount = new ByteArrayInputStream(firebaseConfigEnv.getBytes(StandardCharsets.UTF_8));
                System.out.println("Cargando Firebase desde variable de entorno FIREBASE_CONFIG");
            } else {
                // Si no existe (estamos en local), usamos el archivo serviceAccountKey.json
                serviceAccount = new ClassPathResource("serviceAccountKey.json").getInputStream();
                System.out.println("Cargando Firebase desde archivo local serviceAccountKey.json");
            }

            FirebaseOptions options = FirebaseOptions.builder()
                .setCredentials(GoogleCredentials.fromStream(serviceAccount))
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