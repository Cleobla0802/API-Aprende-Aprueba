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
@CrossOrigin(origins = "*") // Esto permite que Angular o Android se conecten sin bloqueos
public class ApunteController {

	@Autowired
    private IAService serviceIA;

    @PostMapping("/digitalizar")
    public ResponseEntity<Map<String, String>> recibirImagen(@RequestBody Map<String, String> payload) {
        // 1. Extraemos TODOS los datos que envía Angular
        String url = payload.get("url");
        String titulo = payload.get("titulo");
        String userId = payload.get("userId"); // <--- Importante para la privacidad
        String categoria = payload.get("categoria");
        
        // 2. Llamamos a la IA (con el nuevo formato multimodal que pusimos en el Service)
        String textoExtraido = serviceIA.digitalizar(url);
        
        // 3. Guardamos en Firebase (He añadido userId y categoria)
        if (!textoExtraido.startsWith("Error")) {
            // NOTA: Asegúrate de actualizar la firma de este método en tu IAService para que acepte userId y categoria
            serviceIA.guardarEnFirebase(titulo, textoExtraido, url, userId, categoria);
        }
        
        // 4. Devolvemos un JSON real. Esto evita el error "Unexpected token N" en Angular.
        return ResponseEntity.ok(Collections.singletonMap("textoIA", textoExtraido));
    }
    
    // Nueva ruta para cumplir con el requisito de "cada usuario ve lo suyo"
    @GetMapping("/usuario/{uid}")
    public List<Apunte> listarApuntesPorUsuario(@PathVariable String uid) {
        // Deberás filtrar en tu IAService por este UID
        return serviceIA.obtenerApuntesPorUsuario(uid); 
    }
    
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminarApunte(@PathVariable String id) {
        try {
            serviceIA.eliminarDeFirebase(id);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }
}