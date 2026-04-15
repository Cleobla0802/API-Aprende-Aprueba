package com.aprendeaprueba.aprendeaprueba.controller;

import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.aprendeaprueba.aprendeaprueba.model.Resumen;
import com.aprendeaprueba.aprendeaprueba.service.IAService;

@RestController
@RequestMapping("/api/resumenes")
public class ResumenController {

	@Autowired
    private IAService iaService;

    // Endpoint para generar el resumen con la IA
    @PostMapping("/generar")
    public Map<String, String> generar(@RequestBody Map<String, String> payload) {
        String contenido = payload.get("contenido");
        String resultado = iaService.generarResumenTexto(contenido);
        return Map.of("resumen", resultado);
    }

    // Endpoint para guardar el resumen final en Firebase
    @PostMapping("/guardar")
    public Map<String, String> guardar(@RequestBody Resumen resumen) {
        try {
            iaService.guardarResumenFirebase(resumen);
            return Map.of("status", "success", "message", "Resumen guardado correctamente");
        } catch (Exception e) {
            return Map.of("status", "error", "message", e.getMessage());
        }
    }
	
	
}
