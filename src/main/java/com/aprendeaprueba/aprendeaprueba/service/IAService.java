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
import com.google.api.core.ApiFuture;
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
     * 1. Método para hablar con la IA (OpenRouter) - FORMATO MULTIMODAL
     */
    public String digitalizar(String urlImagen) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("Authorization", "Bearer " + apiKey);
            headers.set("X-Title", "AprendeAPrueba App");

            Map<String, Object> body = new HashMap<>();
            body.put("model", modeloIA);

            // --- CONSTRUCCIÓN DEL MENSAJE MULTIMODAL ---
            List<Map<String, Object>> messages = new ArrayList<>();
            Map<String, Object> message = new HashMap<>();
            message.put("role", "user");

            // El contenido ahora es una LISTA de objetos (Tipo Texto y Tipo Image_Url)
            List<Map<String, Object>> contents = new ArrayList<>();

            // Parte 1: El texto (Instrucción)
            Map<String, Object> textPart = new HashMap<>();
            textPart.put("type", "text");
            textPart.put("text", "Transcribe de forma literal y organizada todo el texto de estos apuntes que ves en la imagen.");
            contents.add(textPart);

            // Parte 2: La Imagen (Aquí es donde la IA realmente la "ve")
            Map<String, Object> imagePart = new HashMap<>();
            imagePart.put("type", "image_url");
            Map<String, String> urlObj = new HashMap<>();
            urlObj.put("url", urlImagen);
            imagePart.put("image_url", urlObj);
            contents.add(imagePart);

            message.put("content", contents);
            messages.add(message);
            
            body.put("messages", messages);

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);
            String responseStr = restTemplate.postForObject(urlApiIA, entity, String.class);

            // Extraemos el contenido
            JsonNode root = objectMapper.readTree(responseStr);
            return root.path("choices").get(0).path("message").path("content").asText();

        } catch (Exception e) {
            System.err.println("Error en digitalizar: " + e.getMessage());
            return "Error al digitalizar con IA: " + e.getMessage();
        }
    }

    /**
     * Metodo para guardar el apunte que ha sacado la IA en firebase
     * 
     * @param titulo
     * @param textoIA
     * @param urlImagen
     */
    public void guardarEnFirebase(String titulo, String textoIA, String urlImagen, String userId, String categoria) {
        try {
            // Referencia al nodo "apuntes"
            DatabaseReference ref = FirebaseDatabase.getInstance().getReference("apuntes");

            // Generar ID único
            String apunteId = ref.push().getKey();

            // Creamos el objeto Apunte. 
            // NOTA: Asegúrate de que tu constructor en Apunte.java acepte estos campos o usa setters.
            Apunte nuevoApunte = new Apunte();
            nuevoApunte.setId(apunteId);
            nuevoApunte.setTitulo(titulo);
            nuevoApunte.setContenido(textoIA); // O textoIA, según tu modelo
            nuevoApunte.setUrl(urlImagen);
            nuevoApunte.setUserId(userId);     // <--- Campo clave para la privacidad
            nuevoApunte.setCategoria(categoria);
            nuevoApunte.setFecha(LocalDateTime.now().toString());

            // Guardar en Firebase
            ref.child(apunteId).setValueAsync(nuevoApunte);
            
            System.out.println("Apunte guardado con éxito para el usuario: " + userId);
        } catch (Exception e) {
            System.err.println("Error al guardar en Firebase: " + e.getMessage());
        }
    }
    
    /**
     * 
     * @param uid - Id del usuario cuyos apuntes quiere ver
     * @return promesaApuntes - Una promesa que sera la lista de apuntes a las que le corresponde el usuario
     */
    public List<Apunte> obtenerApuntesPorUsuario(String uid) {
    	// Variable de tipo promesa que se resolvera en un futuro
        CompletableFuture<List<Apunte>> promesaApuntes = new CompletableFuture<>();
        
        Query query = FirebaseDatabase.getInstance() // La clase query nos permite filtrar la ref antes de hacer nada con ella
                .getReference("apuntes")
                .orderByChild("userId") // Ordena la referencia por id del usuario
                .equalTo(uid);

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
    
    /**
     * Metodo que elimina un apunte de la base de datos cuando el usuario lo llame
     * 
     * @param id - Identificador de el apunte que se eliminara
     */
    public void eliminarDeFirebase(String id) {
        try {
            DatabaseReference ref = FirebaseDatabase.getInstance().getReference("apuntes").child(id); // Recoge la referencia de donde se encuentra el apunte a eliminar
            ref.removeValueAsync().get(); // Eliminaria de forma asincrona el valor (el nodo entero), el .get() controla si falla el borrado o no
            System.out.println("Apunte eliminado: " + id);
        } catch (InterruptedException | ExecutionException e) {
            System.err.println("Error al eliminar: " + e.getMessage());
        }
    }
    
    /**
     * 2. Método para generar un resumen de texto usando la IA
     */
    public String generarResumenTexto(String textoApuntes) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("Authorization", "Bearer " + apiKey);

            Map<String, Object> body = new HashMap<>();
            body.put("model", modeloIA);

            List<Map<String, String>> messages = new ArrayList<>();
            messages.add(Map.of("role", "user", "content", 
                "Eres un experto en educación. Resume el siguiente contenido de forma clara, " +
                "estructurada con puntos clave y fácil de estudiar: \n\n" + textoApuntes));

            body.put("messages", messages);

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);
            String responseStr = restTemplate.postForObject(urlApiIA, entity, String.class);

            JsonNode root = objectMapper.readTree(responseStr);
            return root.path("choices").get(0).path("message").path("content").asText();

        } catch (Exception e) {
            return "Error al generar resumen: " + e.getMessage();
        }
    }


    public void guardarResumenFirebase(Resumen resumen) {
        // Referencia al nodo resumenes/ID_USUARIO
        DatabaseReference ref = FirebaseDatabase.getInstance()
            .getReference("resumenes")
            .child(resumen.getUserId());

        String id = ref.push().getKey();
        resumen.setId(id);
        resumen.setFecha(LocalDateTime.now().toString());
        
        // Ahora se guarda en: resumenes/USER_ID/RESUMEN_ID
        ref.child(id).setValueAsync(resumen);
    }

    /**
     * Genera una lista de preguntas tipo test a partir de un texto.
     */
    public List<Pregunta> generarPreguntasIA(String contenido) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("Authorization", "Bearer " + apiKey);

            String prompt = "Actúa como un profesor experto. Basándote SOLAMENTE en el contenido proporcionado, genera 5 preguntas de opción múltiple. " +
                    "Cada pregunta debe tener 3 opciones de respuesta con contenido REAL extraído del texto (no uses letras como A, B o C). " +
                    "Devuelve ÚNICAMENTE un array JSON con este formato exacto: " +
                    "[{\"enunciado\": \"¿Texto de la pregunta?\", \"opciones\": [\"Respuesta real 1\", \"Respuesta real 2\", \"Respuesta real 3\"], \"respuestaCorrecta\": 0}]. " +
                    "Donde 'respuestaCorrecta' es el índice (0, 1 o 2) de la opción válida. " +
                    "No escribas introducciones ni explicaciones, solo el JSON.\n\nContenido: " + contenido;

            Map<String, Object> body = new HashMap<>();
            body.put("model", modeloIA);
            body.put("messages", List.of(Map.of("role", "user", "content", prompt)));

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);
            String responseStr = restTemplate.postForObject(urlApiIA, entity, String.class);

            JsonNode root = objectMapper.readTree(responseStr);
            String jsonPreguntas = root.path("choices").get(0).path("message").path("content").asText();
            
            // Limpiamos el posible markdown (```json ... ```) si la IA lo incluye
            jsonPreguntas = jsonPreguntas.replaceAll("```json", "").replaceAll("```", "").trim();

            return objectMapper.readValue(jsonPreguntas, 
                   objectMapper.getTypeFactory().constructCollectionType(List.class, Pregunta.class));

        } catch (Exception e) {
            System.err.println("Error generando test: " + e.getMessage());
            return new ArrayList<>();
        }
    }

	public void guardarTestFirebase(Test test) {
	    try {
	        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("tests");
	        String id = ref.push().getKey();
	        test.setId(id);
	        test.setFecha(LocalDateTime.now().toString());
	        
	        // .get() fuerza a que el hilo espere hasta que Firebase confirme la escritura
	        ref.child(id).setValueAsync(test).get(); 
	        
	        System.out.println("Test guardado y confirmado en Firebase: " + id);
	    } catch (Exception e) {
	        System.err.println("Error al sincronizar con Firebase: " + e.getMessage());
	    }
	}
}