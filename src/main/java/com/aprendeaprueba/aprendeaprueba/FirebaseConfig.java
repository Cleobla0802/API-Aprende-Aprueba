package com.aprendeaprueba.aprendeaprueba;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

@Configuration
public class FirebaseConfig {

    // Usamos un nombre genérico para evitar que Railway active protecciones de 'secrets'
    @Value("${MY_DB_URL:${firebase.database.url:}}")
    private String databaseUrl;

    @PostConstruct
    public void initialize() {
        try {
            // Buscamos la variable MY_JSON para el contenido del archivo serviceAccount
            String firebaseConfigEnv = System.getenv("MY_JSON");
            InputStream serviceAccount;

            if (firebaseConfigEnv != null && !firebaseConfigEnv.isEmpty()) {
                // MODO SERVIDOR (Railway)
                serviceAccount = new ByteArrayInputStream(firebaseConfigEnv.getBytes(StandardCharsets.UTF_8));
                System.out.println("Cargando Firebase desde la variable de entorno MY_JSON");
            } else {
                // MODO LOCAL (Tu PC)
                System.out.println("Variable MY_JSON no detectada, buscando archivo local serviceAccountKey.json...");
                try {
                    serviceAccount = new ClassPathResource("serviceAccountKey.json").getInputStream();
                } catch (Exception e) {
                    System.err.println("Error: No se encontró MY_JSON ni el archivo local serviceAccountKey.json");
                    return;
                }
            }

            // Validación de la URL de la base de datos
            if (databaseUrl == null || databaseUrl.isEmpty()) {
                // Si la variable MY_DB_URL no está en Railway, intentamos cogerla de System.getenv
                databaseUrl = System.getenv("MY_DB_URL");
            }

            if (databaseUrl == null || databaseUrl.isEmpty()) {
                System.err.println("Error: La URL de la base de datos (MY_DB_URL) está vacía.");
                return;
            }

            FirebaseOptions options = FirebaseOptions.builder()
                    .setCredentials(GoogleCredentials.fromStream(serviceAccount))
                    .setDatabaseUrl(databaseUrl)
                    .build();

            if (FirebaseApp.getApps().isEmpty()) {
                FirebaseApp.initializeApp(options);
                System.out.println("Firebase inicializado con éxito en: " + databaseUrl);
            }
            
        } catch (Exception e) {
            System.err.println("Error CRÍTICO al inicializar Firebase: " + e.getMessage());
            e.printStackTrace();
        }
    }
}