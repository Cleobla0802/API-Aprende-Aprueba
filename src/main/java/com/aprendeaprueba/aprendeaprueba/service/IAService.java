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

    @Value("${IA_API_KEY}")
    private String apiKey;

    @Value("${IA_API_URL}")
    private String urlApiIA;

    @Value("${IA_MODEL}")
    private String modeloIA;

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * UNICO PROCESO DE IA: Digitalizar
     * Este se mantiene porque Android no puede procesar IA pesada.
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
            contents.add(Map.of("type", "text", "text", "Extrae el texto de esta imagen de forma literal:"));
            contents.add(Map.of("type", "image_url", "image_url", Map.of("url", urlImagen)));

            message.put("content", contents);
            messages.add(message);
            body.put("messages", messages);

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);
            String responseStr = restTemplate.postForObject(urlApiIA, entity, String.class);

            JsonNode root = objectMapper.readTree(responseStr);
            return root.path("choices").get(0).path("message").path("content").asText();
        } catch (Exception e) {
            return "Error en proceso de IA: " + e.getMessage();
        }
    }

    /**
     * UNICO METODO DE GUARDADO: Se mantiene para asegurar que el resultado 
     * de la IA se guarde inmediatamente.
     */
    public void guardarEnFirebase(String titulo, String textoIA, String urlImagen, String userId, String categoria) {
        try {
            DatabaseReference ref = FirebaseDatabase.getInstance().getReference("apuntes");
            String id = ref.push().getKey();

            Apunte nuevoApunte = new Apunte();
            nuevoApunte.setId(id);
            nuevoApunte.setTitulo(titulo);
            nuevoApunte.setContenido(textoIA);
            nuevoApunte.setUrl(urlImagen);
            nuevoApunte.setUserId(userId);
            nuevoApunte.setCategoria(categoria);
            nuevoApunte.setFecha(LocalDateTime.now().toString());

            ref.child(id).setValueAsync(nuevoApunte).get();
        } catch (Exception e) {
            System.err.println("Error al guardar: " + e.getMessage());
        }
    }

    /**
     * IA PARA RESUMENES: Devuelve el texto a Android/Angular, 
     * ellos deciden si guardarlo.
     */
    public String generarResumenTexto(String textoApuntes) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(apiKey);

            Map<String, Object> body = new HashMap<>();
            body.put("model", modeloIA);
            body.put("messages", List.of(Map.of("role", "user", "content", "Resume: " + textoApuntes)));

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);
            String responseStr = restTemplate.postForObject(urlApiIA, entity, String.class);
            JsonNode root = objectMapper.readTree(responseStr);
            return root.path("choices").get(0).path("message").path("content").asText();
        } catch (Exception e) {
            return "Error al generar resumen.";
        }
    }

    /**
     * IA PARA TESTS: Devuelve la lista a Android/Angular, 
     * ellos deciden si guardarla.
     */
    public List<Pregunta> generarPreguntasIA(String contenido) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(apiKey);

            String prompt = "Genera 5 preguntas JSON: [{\"enunciado\":\"...\",\"opciones\":[\"...\"],\"respuestaCorrecta\":0}]. Texto: " + contenido;

            Map<String, Object> body = new HashMap<>();
            body.put("model", modeloIA);
            body.put("messages", List.of(Map.of("role", "user", "content", prompt)));

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);
            String responseStr = restTemplate.postForObject(urlApiIA, entity, String.class);

            JsonNode root = objectMapper.readTree(responseStr);
            String json = root.path("choices").get(0).path("message").path("content").asText();
            json = json.replaceAll("```json", "").replaceAll("```", "").trim();

            return objectMapper.readValue(json, objectMapper.getTypeFactory().constructCollectionType(List.class, Pregunta.class));
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }
}