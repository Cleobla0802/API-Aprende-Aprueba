package com.aprendeaprueba.aprendeaprueba.service;

import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.client.RestTemplate;

import com.aprendeaprueba.aprendeaprueba.model.Pregunta;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

@Service
public class IAService {

    @Value("${IA_API_KEY:${ia.api.key}}")
    private String apiKey;

    @Value("${ia.api.url:https://openrouter.ai/api/v1/chat/completions}")
    private String urlOpenRouter;

    @Value("${ia.model.vision:nvidia/nemotron-nano-12b-v2-vl:free}")
    private String modeloVision;

    @Value("${ia.model.texto:nvidia/nemotron-nano-12b-v2-vl:free}")
    private String modeloTexto;

    @Value("${ia.limite.resumen:8000}")
    private int limiteResumen;

    @Value("${ia.limite.test:7000}")
    private int limiteTest;

    @Value("${ia.max-tokens.resumen:1800}")
    private int maxTokensResumen;

    private final RestTemplate restTemplate = crearRestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Extrae texto de una imagen. urlImagen puede ser una URL publica o un data URL base64.
     */
    public String digitalizar(String urlImagen) {
        try {
            HttpEntity<Map<String, Object>> entity = crearEntidad(
                "Extrae solo el texto util para estudiar. Devuelve texto plano, claro y ordenado en espanol de Espana. Ignora marcas de agua, menus o texto no academico.",
                urlImagen,
                modeloVision,
                2500
            );

            String responseStr = restTemplate.postForObject(urlOpenRouter, entity, String.class);
            return extraerContenido(responseStr);
        } catch (Exception e) {
            throw new RuntimeException("Error al digitalizar: " + describirError(e), e);
        }
    }

    /**
     * Genera un resumen de un texto.
     */
    public String generarResumenTexto(String texto) {
        try {
            String textoLimpio = prepararTexto(texto, limiteResumen);
            HttpEntity<Map<String, Object>> entity = crearEntidad(
                "Resume en espanol de Espana, con texto plano, claro y estructurado. No anadas introducciones ni despedidas.\n\nTexto:\n" + textoLimpio,
                null,
                modeloTexto,
                maxTokensResumen
            );

            String responseStr = restTemplate.postForObject(urlOpenRouter, entity, String.class);
            return extraerContenido(responseStr);
        } catch (Exception e) {
            throw new RuntimeException("Error al generar resumen: " + describirError(e), e);
        }
    }

    /**
     * Genera una lista de preguntas tipo test.
     */
    public List<Pregunta> generarPreguntasIA(String contenido, int cantidadPreguntas) {
        Exception ultimoError = null;
        String contenidoLimpio = prepararTexto(contenido, limiteTest);
        int cantidad = Math.max(1, Math.min(cantidadPreguntas, 15));

        for (int intento = 1; intento <= 2; intento++) {
            try {
                String prompt = construirPromptPreguntas(contenidoLimpio, cantidad, intento);
                HttpEntity<Map<String, Object>> entity = crearEntidad(
                    prompt,
                    null,
                    modeloTexto,
                    calcularMaxTokensPreguntas(cantidad)
                );

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
                    .limit(cantidad)
                    .toList();

                if (!preguntas.isEmpty()) {
                    return preguntas;
                }

                throw new RuntimeException("La IA devolvio 0 preguntas validas");
            } catch (Exception e) {
                ultimoError = e;
            }
        }

        throw new RuntimeException("No se pudieron generar preguntas con IA", ultimoError);
    }

    private HttpEntity<Map<String, Object>> crearEntidad(String prompt, String urlImagen, String modelo, int maxTokens) {
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException("Falta configurar IA_API_KEY");
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(apiKey.trim());
        headers.set("HTTP-Referer", "https://aprendeaprueba.render.com");
        headers.set("X-Title", "AprendeAAprueba");

        Map<String, Object> body = new HashMap<>();
        body.put("model", modelo);

        List<Map<String, Object>> messages = new ArrayList<>();
        Map<String, Object> message = new HashMap<>();
        message.put("role", "user");

        if (urlImagen != null) {
            List<Map<String, Object>> contents = new ArrayList<>();
            contents.add(Map.of("type", "text", "text", prompt));
            contents.add(Map.of("type", "image_url", "image_url", Map.of("url", urlImagen)));
            message.put("content", contents);
        } else {
            message.put("content", prompt);
        }

        messages.add(message);
        body.put("messages", messages);
        body.put("temperature", 0.2);
        body.put("max_tokens", maxTokens);

        return new HttpEntity<>(body, headers);
    }

    private String extraerContenido(String responseStr) throws Exception {
        JsonNode root = objectMapper.readTree(responseStr);
        JsonNode errorNode = root.path("error");
        if (!errorNode.isMissingNode() && !errorNode.isNull()) {
            throw new RuntimeException(errorNode.path("message").asText("Error devuelto por la IA"));
        }

        JsonNode choices = root.path("choices");
        if (!choices.isArray() || choices.isEmpty()) {
            throw new RuntimeException("La IA no devolvio ninguna respuesta");
        }

        return choices.get(0).path("message").path("content").asText();
    }

    private String describirError(Exception e) {
        if (e instanceof RestClientResponseException restError) {
            String body = restError.getResponseBodyAsString();
            String mensaje = extraerMensajeError(body);
            if (!mensaje.isBlank()) {
                return restError.getStatusCode() + " - " + mensaje;
            }
            return restError.getStatusCode().toString();
        }

        return e.getMessage() == null ? "Error desconocido" : e.getMessage();
    }

    private String extraerMensajeError(String body) {
        if (body == null || body.isBlank()) {
            return "";
        }

        try {
            JsonNode root = objectMapper.readTree(body);
            JsonNode error = root.path("error");
            if (error.isTextual()) {
                return error.asText();
            }
            if (!error.isMissingNode() && !error.isNull()) {
                String message = error.path("message").asText("");
                if (!message.isBlank()) {
                    return message;
                }
            }
            String message = root.path("message").asText("");
            return message.isBlank() ? body : message;
        } catch (Exception ignored) {
            return body;
        }
    }

    private String construirPromptPreguntas(String contenido, int cantidadPreguntas, int intento) {
        String reglaExtra = intento == 1
            ? ""
            : "- Si dudas, simplifica las preguntas y evita texto largo.\n";

        return """
            Devuelve solo JSON valido, sin markdown ni explicaciones.
            Forma exacta: {"preguntas":[{"enunciado":"","opciones":["","","",""],"respuestaCorrecta":0}]}
            Reglas:
            - Exactamente %d preguntas.
            - Exactamente 4 opciones por pregunta.
            - respuestaCorrecta debe ser 0, 1, 2 o 3.
            - Espanol de Espana.
            - Usa solo el texto.
            %s
            Texto:
            %s
            """.formatted(cantidadPreguntas, reglaExtra, contenido);
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

    private String prepararTexto(String texto, int limite) {
        if (texto == null) return "";

        String limpio = texto
            .replaceAll("\\s+", " ")
            .trim();

        return limpio.substring(0, Math.min(limpio.length(), limite));
    }

    private int calcularMaxTokensPreguntas(int cantidad) {
        if (cantidad <= 5) return 1200;
        if (cantidad <= 10) return 2200;
        return 3200;
    }

    private RestTemplate crearRestTemplate() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofSeconds(10));
        factory.setReadTimeout(Duration.ofSeconds(65));
        return new RestTemplate(factory);
    }
}
