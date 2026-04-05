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
                .orderByChild("usuarioId") // Ordena la referencia por id del usuario
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
}