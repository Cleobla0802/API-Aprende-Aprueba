package com.aprendeaprueba.aprendeaprueba.service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import com.aprendeaprueba.aprendeaprueba.model.Apunte;
import com.aprendeaprueba.aprendeaprueba.model.Pregunta;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

@Service
public class IAService {

    @Value("${IA_API_KEY:${ia.api.key}}")
    private String apiKey;

    @Value("${IA_API_URL:${ia.api.url:https://api.openai.com/v1/chat/completions}}")
    private String urlApiIA;

    @Value("${IA_MODEL:${ia.model:gpt-4o}}")
    private String modeloIA;

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Procesa la imagen con la IA de NVIDIA/OpenAI
     */
    public String digitalizar(String urlImagen) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(apiKey);
            headers.set("Accept", "application/json");

            Map<String, Object> body = new HashMap<>();
            body.put("model", modeloIA);
            body.put("max_tokens", 1024);

            List<Map<String, Object>> messages = new ArrayList<>();
            Map<String, Object> message = new HashMap<>();
            message.put("role", "user");

            List<Map<String, Object>> contents = new ArrayList<>();
            contents.add(Map.of("type", "text", "text", "Extrae el texto de esta imagen de forma literal y organizada:"));
            contents.add(Map.of("type", "image_url", "image_url", Map.of("url", urlImagen)));

            message.put("content", contents);
            messages.add(message);
            body.put("messages", messages);

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);
            String responseStr = restTemplate.postForObject(urlApiIA, entity, String.class);

            JsonNode root = objectMapper.readTree(responseStr);
            return root.path("choices").get(0).path("message").path("content").asText();

        } catch (Exception e) {
            System.err.println("Error en IA: " + e.getMessage());
            return "Error: No se pudo procesar la imagen.";
        }
    }

    /**
     * Genera un resumen y devuelve el texto (Sin guardar en Firebase)
     */
    public String generarResumenTexto(String textoApuntes) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(apiKey);

            Map<String, Object> body = new HashMap<>();
            body.put("model", modeloIA);
            body.put("messages", List.of(
                Map.of("role", "user", "content", "Resume de forma estructurada: \n\n" + textoApuntes)
            ));

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);
            String responseStr = restTemplate.postForObject(urlApiIA, entity, String.class);
            JsonNode root = objectMapper.readTree(responseStr);
            return root.path("choices").get(0).path("message").path("content").asText();
        } catch (Exception e) {
            return "Error al generar resumen.";
        }
    }

    /**
     * Genera preguntas y devuelve la lista (Sin guardar en Firebase)
     */
    public List<Pregunta> generarPreguntasIA(String contenido) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(apiKey);

            String prompt = "Genera 5 preguntas tipo test en JSON: " +
                            "[{\"enunciado\": \"...\", \"opciones\": [\"...\"], \"respuestaCorrecta\": 0}]. " +
                            "Texto: " + contenido;

            Map<String, Object> body = new HashMap<>();
            body.put("model", modeloIA);
            body.put("messages", List.of(Map.of("role", "user", "content", prompt)));

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);
            String responseStr = restTemplate.postForObject(urlApiIA, entity, String.class);

            JsonNode root = objectMapper.readTree(responseStr);
            String jsonStr = root.path("choices").get(0).path("message").path("content").asText();
            jsonStr = jsonStr.replaceAll("```json", "").replaceAll("```", "").trim();

            return objectMapper.readValue(jsonStr, 
                   objectMapper.getTypeFactory().constructCollectionType(List.class, Pregunta.class));
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }

    /**
     * UNICO METODO DE ESCRITURA: Guardar el apunte principal
     */
    public void guardarApunteFirebase(String titulo, String contenido, String url, String userId, String categoria) {
        try {
            DatabaseReference ref = FirebaseDatabase.getInstance().getReference("apuntes");
            String id = ref.push().getKey();

            Apunte apunte = new Apunte();
            apunte.setId(id);
            apunte.setTitulo(titulo);
            apunte.setContenido(contenido);
            apunte.setUrl(url);
            apunte.setUserId(userId);
            apunte.setCategoria(categoria);
            apunte.setFecha(LocalDateTime.now().toString());

            // Usamos get() para asegurar que Render no cierre la conexión antes de guardar
            ref.child(id).setValueAsync(apunte).get();
            System.out.println("Apunte guardado: " + id);
        } catch (Exception e) {
            System.err.println("Error guardando apunte: " + e.getMessage());
        }
    }
}