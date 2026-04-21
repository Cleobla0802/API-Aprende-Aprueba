package com.aprendeaprueba.aprendeaprueba.service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import com.aprendeaprueba.aprendeaprueba.model.Apunte;
import com.aprendeaprueba.aprendeaprueba.model.Pregunta;
import com.aprendeaprueba.aprendeaprueba.model.Resumen;
import com.aprendeaprueba.aprendeaprueba.model.Test;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

@Service
public class IAService {

    @Value("${ia.api.key}")
    private String apiKey;

    @Value("${ia.api.url}")
    private String urlApiIA;

    @Value("${ia.model}")
    private String modeloIA;

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 1. Digitalizar Imagen (Formato Multimodal OpenAI)
     */
    public String digitalizar(String urlImagen) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(apiKey); // Reemplaza "Authorization: Bearer" manual por este método más limpio

            Map<String, Object> body = new HashMap<>();
            body.put("model", modeloIA);

            // Mensaje Multimodal
            List<Map<String, Object>> messages = new ArrayList<>();
            Map<String, Object> message = new HashMap<>();
            message.put("role", "user");

            List<Map<String, Object>> contents = new ArrayList<>();

            // Texto descriptivo
            contents.add(Map.of("type", "text", "text", "Transcribe de forma literal y organizada todo el texto de estos apuntes que ves en la imagen."));

            // Imagen
            contents.add(Map.of(
                "type", "image_url",
                "image_url", Map.of("url", urlImagen)
            ));

            message.put("content", contents);
            messages.add(message);
            body.put("messages", messages);

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);
            String responseStr = restTemplate.postForObject(urlApiIA, entity, String.class);

            JsonNode root = objectMapper.readTree(responseStr);
            return root.path("choices").get(0).path("message").path("content").asText();

        } catch (Exception e) {
            System.err.println("Error en digitalizar: " + e.getMessage());
            return "Error al digitalizar con OpenAI: " + e.getMessage();
        }
    }

    /**
     * 2. Generar Resumen
     */
    public String generarResumenTexto(String textoApuntes) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(apiKey);

            Map<String, Object> body = new HashMap<>();
            body.put("model", modeloIA);

            List<Map<String, String>> messages = List.of(
                Map.of("role", "system", "content", "Eres un experto en educación."),
                Map.of("role", "user", "content", "Resume el siguiente contenido de forma clara y estructurada: \n\n" + textoApuntes)
            );

            body.put("messages", messages);

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);
            String responseStr = restTemplate.postForObject(urlApiIA, entity, String.class);

            JsonNode root = objectMapper.readTree(responseStr);
            return root.path("choices").get(0).path("message").path("content").asText();

        } catch (Exception e) {
            return "Error al generar resumen: " + e.getMessage();
        }
    }

    /**
     * 3. Generar Test (JSON)
     */
    public List<Pregunta> generarPreguntasIA(String contenido) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(apiKey);

            String prompt = "Genera 5 preguntas de opción múltiple a partir del texto. " +
                            "Devuelve SOLO un array JSON con este formato: " +
                            "[{\"enunciado\": \"...\", \"opciones\": [\"...\", \"...\", \"...\"], \"respuestaCorrecta\": 0}]. " +
                            "Contenido: " + contenido;

            Map<String, Object> body = new HashMap<>();
            body.put("model", modeloIA);
            body.put("messages", List.of(Map.of("role", "user", "content", prompt)));

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);
            String responseStr = restTemplate.postForObject(urlApiIA, entity, String.class);

            JsonNode root = objectMapper.readTree(responseStr);
            String jsonPreguntas = root.path("choices").get(0).path("message").path("content").asText();
            
            // Limpiar Markdown
            jsonPreguntas = jsonPreguntas.replaceAll("```json", "").replaceAll("```", "").trim();

            return objectMapper.readValue(jsonPreguntas, 
                   objectMapper.getTypeFactory().constructCollectionType(List.class, Pregunta.class));

        } catch (Exception e) {
            System.err.println("Error generando test: " + e.getMessage());
            return new ArrayList<>();
        }
    }

    // --- MÉTODOS DE FIREBASE ---

    public void guardarEnFirebase(String titulo, String textoIA, String urlImagen, String userId, String categoria) {
        try {
            DatabaseReference ref = FirebaseDatabase.getInstance().getReference("apuntes");
            String apunteId = ref.push().getKey();

            Apunte nuevoApunte = new Apunte();
            nuevoApunte.setId(apunteId);
            nuevoApunte.setTitulo(titulo);
            nuevoApunte.setContenido(textoIA);
            nuevoApunte.setUrl(urlImagen);
            nuevoApunte.setUserId(userId);
            nuevoApunte.setCategoria(categoria);
            nuevoApunte.setFecha(LocalDateTime.now().toString());

            ref.child(apunteId).setValueAsync(nuevoApunte);
        } catch (Exception e) {
            System.err.println("Error al guardar en Firebase: " + e.getMessage());
        }
    }

    public List<Apunte> obtenerApuntesPorUsuario(String uid) {
        CompletableFuture<List<Apunte>> promesaApuntes = new CompletableFuture<>();
        Query query = FirebaseDatabase.getInstance().getReference("apuntes").orderByChild("userId").equalTo(uid);

        query.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                List<Apunte> lista = new ArrayList<>();
                for (DataSnapshot ds : dataSnapshot.getChildren()) {
                    Apunte apunte = ds.getValue(Apunte.class);
                    lista.add(apunte);
                }
                promesaApuntes.complete(lista);
            }
            @Override
            public void onCancelled(DatabaseError error) {
                promesaApuntes.completeExceptionally(error.toException());
            }
        });

        try {
            return promesaApuntes.get();
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }

    public void eliminarDeFirebase(String id) {
        try {
            DatabaseReference ref = FirebaseDatabase.getInstance().getReference("apuntes").child(id);
            ref.removeValueAsync().get();
        } catch (InterruptedException | ExecutionException e) {
            System.err.println("Error al eliminar: " + e.getMessage());
        }
    }

    public void guardarResumenFirebase(Resumen resumen) {
        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("resumenes").child(resumen.getUserId());
        String id = ref.push().getKey();
        resumen.setId(id);
        resumen.setFecha(LocalDateTime.now().toString());
        ref.child(id).setValueAsync(resumen);
    }

    public void guardarTestFirebase(Test test) {
        try {
            DatabaseReference ref = FirebaseDatabase.getInstance().getReference("tests");
            String id = ref.push().getKey();
            test.setId(id);
            test.setFecha(LocalDateTime.now().toString());
            ref.child(id).setValueAsync(test).get(); 
        } catch (Exception e) {
            System.err.println("Error al guardar test: " + e.getMessage());
        }
    }
}