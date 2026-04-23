package com.aprendeaprueba.aprendeaprueba.service;

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

import com.aprendeaprueba.aprendeaprueba.model.Pregunta;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

@Service
public class IAService {

    @Value("${IA_API_KEY:${ia.api.key}}")
    private String apiKey;

    // URL fija para OpenRouter
    private final String urlOpenRouter = "https://openrouter.ai/api/v1/chat/completions";
    
    // Modelo gratuito de NVIDIA en OpenRouter
    private final String modeloIA = "nvidia/nemotron-nano-12b-v2-vl:free";

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Extrae texto de una imagen (OCR Multimodal)
     */
    public String digitalizar(String urlImagen) {
        try {
            HttpEntity<Map<String, Object>> entity = crearEntidad(
                "Extrae el texto de esta imagen de forma literal y organizada (en español de españa):", 
                urlImagen
            );

            String responseStr = restTemplate.postForObject(urlOpenRouter, entity, String.class);
            return extraerContenido(responseStr);
        } catch (Exception e) {
            return "Error al digitalizar: " + e.getMessage();
        }
    }

    /**
     * Genera un resumen de un texto
     */
    public String generarResumenTexto(String texto) {
        try {
            HttpEntity<Map<String, Object>> entity = crearEntidad(
                "Resume el siguiente contenido de forma clara y estructurada: " + texto, 
                null
            );

            String responseStr = restTemplate.postForObject(urlOpenRouter, entity, String.class);
            return extraerContenido(responseStr);
        } catch (Exception e) {
            return "Error al generar resumen: " + e.getMessage();
        }
    }

    /**
     * Genera una lista de preguntas tipo test
     */
    public List<Pregunta> generarPreguntasIA(String contenido) {
        try {
            String prompt = "Genera 5 preguntas tipo test basadas en el siguiente texto. " +
                    "Responde ÚNICAMENTE con un JSON puro con este formato: " +
                    "[{\"enunciado\": \"...\", \"opciones\": [\"...\"], \"respuestaCorrecta\": 0}]. " +
                    "Texto: " + contenido;

            HttpEntity<Map<String, Object>> entity = crearEntidad(prompt, null);
            String responseStr = restTemplate.postForObject(urlOpenRouter, entity, String.class);
            String jsonStr = extraerContenido(responseStr);

            // Limpieza básica del JSON por si la IA añade bloques de código
            jsonStr = jsonStr.replaceAll("```json", "").replaceAll("```", "").trim();

            return objectMapper.readValue(jsonStr, 
                   objectMapper.getTypeFactory().constructCollectionType(List.class, Pregunta.class));
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }

    // --- MÉTODOS AUXILIARES PARA EVITAR REPETIR CÓDIGO ---

    private HttpEntity<Map<String, Object>> crearEntidad(String prompt, String urlImagen) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(apiKey.trim());
        // Cabeceras obligatorias para OpenRouter
        headers.set("HTTP-Referer", "https://aprendeaprueba.render.com"); 
        headers.set("X-Title", "AprendeAAprueba");

        Map<String, Object> body = new HashMap<>();
        body.put("model", modeloIA);

        List<Map<String, Object>> messages = new ArrayList<>();
        Map<String, Object> message = new HashMap<>();
        message.put("role", "user");

        if (urlImagen != null) {
            // Formato Multimodal para imágenes
            List<Map<String, Object>> contents = new ArrayList<>();
            contents.add(Map.of("type", "text", "text", prompt));
            contents.add(Map.of("type", "image_url", "image_url", Map.of("url", urlImagen)));
            message.put("content", contents);
        } else {
            // Formato solo texto
            message.put("content", prompt);
        }

        messages.add(message);
        body.put("messages", messages);

        return new HttpEntity<>(body, headers);
    }

    private String extraerContenido(String responseStr) throws Exception {
        JsonNode root = objectMapper.readTree(responseStr);
        return root.path("choices").get(0).path("message").path("content").asText();
    }
}