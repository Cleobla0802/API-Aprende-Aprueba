package com.aprendeaprueba.aprendeaprueba.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.aprendeaprueba.aprendeaprueba.model.Apunte;
import com.aprendeaprueba.aprendeaprueba.service.IAService;

import java.util.Collections;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/apuntes")
public class ApunteController {

	@Autowired
    private IAService serviceIA;

    @PostMapping("/digitalizar")
    public ResponseEntity<Map<String, String>> recibirImagen(@RequestBody Map<String, String> payload) {
        // 1. Usamos nombres consistentes (Asegúrate que Android envíe "url")
        String url = payload.get("url"); 
        String titulo = payload.get("titulo");
        String userId = payload.get("userId");
        String categoria = payload.get("categoria");
        String dificultad = payload.get("dificultad");

        // Validación básica para evitar NullPointer
        if (url == null || url.isEmpty()) {
            return ResponseEntity.badRequest().body(Collections.singletonMap("error", "La URL de la imagen es obligatoria"));
        }

        try {
            // 2. Llamamos a la IA
            String textoExtraido = serviceIA.digitalizar(url);

            if (textoExtraido == null || textoExtraido.startsWith("Error")) {
                return ResponseEntity.status(500).body(Collections.singletonMap("error", "La IA no pudo procesar la imagen"));
            }

            // 3. Guardamos en Firebase
            serviceIA.guardarEnFirebase(titulo, textoExtraido, url, userId, categoria);

            // 4. Devolvemos respuesta exitosa
            return ResponseEntity.ok(Collections.singletonMap("textoIA", textoExtraido));

        } catch (Exception e) {
            return ResponseEntity.status(500).body(Collections.singletonMap("error", e.getMessage()));
        }
    }

    @GetMapping("/usuario/{uid}")
    public ResponseEntity<List<Apunte>> listarApuntesPorUsuario(@PathVariable String uid) {
        try {
            List<Apunte> apuntes = serviceIA.obtenerApuntesPorUsuario(uid);
            // Si la lista es null, devolvemos lista vacía para evitar error 500
            return ResponseEntity.ok(apuntes != null ? apuntes : Collections.emptyList());
        } catch (Exception e) {
            // Esto te ayudará a ver por qué fallaba antes en los logs
            e.printStackTrace(); 
            return ResponseEntity.status(500).build();
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminarApunte(@PathVariable String id) {
        try {
            serviceIA.eliminarDeFirebase(id);
            return ResponseEntity.noContent().build(); // 204 es más correcto para Delete
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }
}