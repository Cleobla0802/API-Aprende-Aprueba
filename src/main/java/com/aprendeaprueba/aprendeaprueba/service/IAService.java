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
                "Extrae el texto de esta imagen de forma literal y organizada, intenta poner texto completamente plano y ademas de eso bien formado, solamente extrae lo que creas que es texto relacionado con estudiar si ves texto de alguna marca ignoralo (en español de españa):", 
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
                "Resume el siguiente contenido de forma clara y estructurada, intenta poner texto completamente plano sin ningun tipo de añadido (en español de españa): " + texto, 
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
    public List<Pregunta> generarPreguntasIA(String contenido, int cantidadPreguntas) {
        Exception ultimoError = null;

        for (int intento = 1; intento <= 2; intento++) {
            try {
                String prompt = construirPromptPreguntas(contenido, cantidadPreguntas, intento);
                HttpEntity<Map<String, Object>> entity = crearEntidad(prompt, null);

                String responseStr = restTemplate.postForObject(urlOpenRouter, entity, String.class);
                String respuestaIA = extraerContenido(responseStr);
                String jsonStr = extraerJson(respuestaIA);

                JsonNode root = objectMapper.readTree(jsonStr);
                JsonNode preguntasNode = root.isArray() ? root : root.path("preguntas");

                List<Pregunta> preguntas = objectMapper.convertValue(
                    preguntasNode,
                    objectMapper.getTypeFactory().constructCollectionType(List.class, Pregunta.class)
                );

                preguntas = preguntas.stream()
                    .filter(p -> p.getEnunciado() != null && !p.getEnunciado().isBlank())
                    .filter(p -> p.getOpciones() != null && p.getOpciones().size() == 4)
                    .filter(p -> p.getRespuestaCorrecta() >= 0 && p.getRespuestaCorrecta() <= 3)
                    .limit(cantidadPreguntas)
                    .toList();

                if (!preguntas.isEmpty()) {
                    return preguntas;
                }

                throw new RuntimeException("La IA devolvió 0 preguntas válidas");
            } catch (Exception e) {
                ultimoError = e;
            }
        }

        throw new RuntimeException("No se pudieron generar preguntas con IA", ultimoError);
    }


    private String limpiarJson(String texto) {
        return texto
            .replace("```json", "")
            .replace("```", "")
            .trim();
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
        body.put("temperature", 0.2);

        return new HttpEntity<>(body, headers);
    }

    private String extraerContenido(String responseStr) throws Exception {
        JsonNode root = objectMapper.readTree(responseStr);
        return root.path("choices").get(0).path("message").path("content").asText();
    }
    
    private String construirPromptPreguntas(String contenido, int cantidadPreguntas, int intento) {
        return """
            Devuelve SOLO JSON válido. No escribas explicaciones, markdown ni texto adicional.

            Formato exacto:
            {
              "preguntas": [
                {
                  "enunciado": "texto",
                  "opciones": ["opción A", "opción B", "opción C", "opción D"],
                  "respuestaCorrecta": 0
                }
              ]
            }

            Reglas obligatorias:
            - Genera exactamente %d preguntas.
            - Cada pregunta debe tener exactamente 4 opciones.
            - respuestaCorrecta debe ser un número entre 0 y 3.
            - Todo debe estar en español de España.
            - Basa las preguntas únicamente en el texto.

            Texto:
            %s
            """.formatted(cantidadPreguntas, contenido);
    }
    
    private String extraerJson(String texto) {
        String limpio = texto
            .replace("```json", "")
            .replace("```", "")
            .trim();

        int inicioObjeto = limpio.indexOf("{");
        int finObjeto = limpio.lastIndexOf("}");

        if (inicioObjeto >= 0 && finObjeto > inicioObjeto) {
            return limpio.substring(inicioObjeto, finObjeto + 1);
        }

        int inicioArray = limpio.indexOf("[");
        int finArray = limpio.lastIndexOf("]");

        if (inicioArray >= 0 && finArray > inicioArray) {
            return limpio.substring(inicioArray, finArray + 1);
        }

        return limpio;
    }    
}